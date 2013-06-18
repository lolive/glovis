#!/usr/bin/perl -wT
# This cgi returns the request browse image, scaled to the requested size and
# annotated with the requested map layers.

# use strict mode
use strict;

# add the directory for the GD library
use lib "perllib";

# use the GD and CGI modules
use GD;
use GD::Polyline;
use CGI qw(:standard escapeHTML);

# use the least square fit module
use lsq;

# flush stdout after each line
$| = 1;

# get the scene id passed in
my $sceneid = param('scene_id');
$_ = $sceneid;

# make sure it is really a scene id
if (!m/^L[OCETM]\d{14}\w{3}\d{2}$/)
{
    handleerror();
    exit;
}

# Note: O = OLI (L8), C = Combined OLI/TIRS (L8), E = ETM (L7), M = MSS (L4-5)
# T = TM if sensor is 4 or 5, could be TIRS if 8 but GloVis doesn't use thermal

# get the directory where the metadata file is and the name of the metadata file
my ($dirname, $metafile) = getMetadataFileName($sceneid);

# read the metadata file to get the name of the browse image and the corner
# coordinates of the image
my ($imageName, $cornerLats, $cornerLongs) = readMetaFile($metafile);

# build the path to the image
$imageName = $dirname . "/" . $imageName;

# attach ".jpg" if it isn't already there since some have it and others don't
$_ = $imageName;
$imageName = $imageName . ".jpg" if (not /.jpg$/);

# draw the browse image
drawImage($imageName, $cornerLats, $cornerLongs);

exit 0;

##############################################################################
# Name: getMetadataFileName
#
# Description: Using the scene id, finds the correct directory for the
#   metadata file and the metadata file name.
#
# Inputs:
#   scene id - the scene id for the metadata file
#
# Returns:
#   The directory for the metadata file
#   The name of the metadata file
#
##############################################################################
sub getMetadataFileName
{
    my $scene_id = $_[0];

    # extract the components needed from the scene id to build the path to the 
    # actual image
    my $sensor = substr($sceneid,2,1);
    my $path = substr($sceneid,3,3);
    my $row = substr($sceneid,6,3);
    my $year = substr($sceneid,9,4);
    my $mode = substr($sceneid,1,1);

    # make a version of the sensor for the path to the image since L4 and 5 are
    # both under the l5 directory
    my $sensorpath = $sensor;
    $sensorpath = "5" if ($sensor == 4);
    $sensorpath .= "oli" if ($sensor == 8); # Landsat 8 path is l8oli

    # Because the TM and MSS sensor are aboard Landsat 4 and 5 satellites we
    # have to use the sensor number and the instrument mode to find the correct
    # directory path for the right sensor type: (4/5 TM = /l5),(4/5 MSS =
    # /l4_5mss), & (ETM = /l7)
    if (($sensorpath == 5) && ($mode eq "M"))
    {
        $sensorpath = "4_5mss";  
    }
    elsif (($sensor <= 3) && ($mode eq "M"))
    {
        $sensorpath = "1_3mss";
    }

    # build the directory name for this sensor/path/row/year
    my $dirname = "l" . $sensorpath . "/p" . $path . "/r" . $row . "/y" . $year;
    my $metafile = $dirname . "/" . $scene_id . ".meta";

    if ($sensor == 7)
    {
        # for Landsat 7, read the original scene file name from the file.  For
        # some reason the LAM used very dumb names for the early L7 browse
        # scenes, so this is the generic method to pick the proper name for the
        # file

        # if the metadata file doesn't exist, look in the SLC-off inventory
        # instead
        if (! -e $metafile)
        {
            if ($year >= 2003)
            {
                my $slc_off_dirname = "l7slc_off/p" . $path . "/r" . $row . 
                    "/y" . $year;
                my $alternate_metafile = $slc_off_dirname . "/" . $scene_id
                        . ".meta";
                if (-e $alternate_metafile)
                {
                    $metafile = $alternate_metafile;
                    $dirname = $slc_off_dirname;
                }
            }
        }
    }

    return $dirname, $metafile;
}

##############################################################################
# Name: calcLineSampleCoefs
#
# Description: Using the latitude and longitude corner values of the browse
#   image, calculate the equation coefficients needed to calculate the
#   line/sample of any lat/long in the image.
#
# Inputs:
#   cornerLats - Array of the four corner latitudes (UL, UR, LL, LR)
#   cornerLongs - Array of the four corner longitudes (UL, UR, LL, LR)
#   lines - Array of line numbers of the four corners
#   samples - Array of sample numbers of the four corners
#   lineCoefs - reference to array of line coefficients to return
#   sampleCoefs - reference to array of sample coefficients to return
#
# Returns:
#   lineCoefs - coefficients used to calculate the image line from a lat/long
#   sampleCoefs-coefficients used to calculate the image sample from a lat/long
#
##############################################################################
sub calcLineSampleCoefs($$$$$$)
{
    my $cornerLats = $_[0];
    my $cornerLongs = $_[1];
    my $lines = $_[2];
    my $samples = $_[3];
    my $lineCoefs = $_[4];
    my $sampleCoefs = $_[5];

    @$sampleCoefs = @$samples;
    @$lineCoefs = @$lines;

    # account for scenes that span the +/-180 degrees of longitude line
    if ($$cornerLongs[0] < -100.0)
    {
        $$cornerLongs[1] -= 360.0 if ($$cornerLongs[1] > 100);
        $$cornerLongs[2] -= 360.0 if ($$cornerLongs[2] > 100);
        $$cornerLongs[3] -= 360.0 if ($$cornerLongs[3] > 100);
    }
    elsif ($$cornerLongs[0] > 100.0)
    {
        $$cornerLongs[1] += 360.0 if ($$cornerLongs[1] < -100);
        $$cornerLongs[2] += 360.0 if ($$cornerLongs[2] < -100);
        $$cornerLongs[3] += 360.0 if ($$cornerLongs[3] < -100);
    }

    # define the equation values for the four corners
    my @A = ();
    for (my $i = 0; $i < 4; $i++)
    {
        $A[$i] = 1.0;
        $A[$i+4] = $$cornerLongs[$i];
        $A[$i+8] = $$cornerLats[$i];
        $A[$i+12] = $$cornerLongs[$i] * $$cornerLats[$i];
    }
    my @v = (0.0, 0.0, 0.0, 0.0);

    # solve the equations for the coefficients
    QR(\@A, 4, 4, \@v);
    QR_solve(\@A, 4, 4, \@v, $sampleCoefs, 0);
    QR_solve(\@A, 4, 4, \@v, $lineCoefs, 0);
}

##############################################################################
# Name: to15Degrees
#
# Description: Convert a latitude or longitude to a multiple of 15 degrees
#   to determine the name of the map layer files needed.
#
# Inputs:
#   latitude or longitude value
#
# Returns:
#   nearest multiple of 15 degrees for the passed in value
#
##############################################################################
sub to15Degrees($)
{
    my $value = $_[0];

    $value += 0.99 if ($value >= 0);
    $value -= 0.99 if ($value < 0);
    my $intValue = int($value);
    my $tileIndex = int($intValue/15);
    my $rem = $intValue % 15;
    $tileIndex-- if (($intValue < 0) && ($rem != 0));

    $tileIndex *= 15;

    return $tileIndex;
}

##############################################################################
# Name: get15DegreeTiles
#
# Description: For a given lat/long bounding box, return the list of map 
#   layer tiles that intersect the area.
#
# Inputs:
#   ulLat - upper-left corner latitude of the minbox of the covered area
#   ulLong - upper-left corner longitude of the minbox of the covered area
#   lrLat - lower-right corner latitude of the minbox of the covered area
#   lrLong - lower-right corner longitude of the minbox of the covered area
#
# Returns:
#   A reference to an array of the base tile names covered by the area 
#   (i.e. N30W105)
#
##############################################################################
sub get15DegreeTiles($$$$)
{
    my $ulLat = $_[0];
    my $ulLong = $_[1];
    my $lrLat = $_[2];
    my $lrLong = $_[3];

    my $left = to15Degrees($ulLong - 1.0);
    my $right = to15Degrees($lrLong + 1.0);
    my $top = to15Degrees($ulLat + 1.0);
    my $bottom = to15Degrees($lrLat - 1.0);
    
    # shift the latitude values from the lower-left to the upper-left corner
    # of the tile name
    $top += 15;
    $bottom += 15;

    # detect wraparound on the longitude and handle it
    if ((($right - $left) > 180) || (($left - $right) > 180))
    {
        $left = 165;
        $right = 180;
    }

    # create the names
    my @tileNames = ();
    for (my $lon = $left; $lon <= $right; $lon += 15)
    {
        for (my $lat = $top; $lat >= $bottom; $lat -= 15)
        {
            my $name;
            if ($lat < 0)
            {
                $name = "S";
            }
            else
            {
                $name = "N";
            }

            $name .= abs($lat);

            if ($lon < 0)
            {
                $name .= "W";
            }
            else
            {
                $name .= "E";
            }
            $name .= abs($lon);

            push(@tileNames, $name);
        }
    }

    # return the list of tiles
    return \@tileNames;
}

##############################################################################
# Name: sortOnCitySize
#
# Description: routine used to sort the cities based on the relative size
#   indication (first member of the array).
#
# Inputs:
#   The special $a and $b values passed to sort routines
#
# Returns:
#   -1 if the first city is larger than the second
#    0 if the cities are the same relative size
#    1 if the first city is smaller than the second
#
##############################################################################
sub sortOnCitySize
{
    return -1 if ($$a[0] < $$b[0]);
    return 1 if ($$a[0] > $$b[0]);
    return 0;
}

##############################################################################
# Name: readCities
#
# Description: Reads the cities in the covered area and returns a reference to
#   the list.
#
# Inputs:
#   ulLat - upper-left corner latitude of the minbox of the covered area
#   ulLong - upper-left corner longitude of the minbox of the covered area
#   lrLat - lower-right corner latitude of the minbox of the covered area
#   lrLong - lower-right corner longitude of the minbox of the covered area
#
# Returns:
#   A reference to an array of cities.  Each city entry is a reference to 
#   an array that holds the relative size, latitude, longitude, and city name.
#
##############################################################################
sub readCities($$$$)
{
    my $ulLat = $_[0];
    my $ulLong = $_[1];
    my $lrLat = $_[2];
    my $lrLong = $_[3];

    my @cities = ();

    # determine the tiles for the requested area
    my $tileNames = get15DegreeTiles($ulLat, $ulLong, $lrLat, $lrLong);

    # read the file for each tile
    foreach my $tileName (@$tileNames)
    {
        # build the filename
        my $fileName = "linework/" . $tileName . "City.1000";

        # open the city file
        my $status = open(CITIES, "<$fileName");

        # if the open did not succeed, assume there is no file for this tile
        # and continue with the next tile
        next if (!$status);

        # read the first 50 cities from the file that fall in the requested
        # area
        my $count = 0;
        while(my $line = <CITIES>)
        {
            chomp($line);
            my @data = split(',', $line);
            next if ($#data != 3);

            # filter out cities not in the bounding box
            my $lat = $data[1];
            my $lon = $data[2];

            next if ($lat > $ulLat);
            next if ($lat < $lrLat);
            next if ($lon < $ulLong);
            next if ($lon > $lrLong);

            push(@cities, \@data);
            $count++;

            # quite after 50 cities are read from the file in the area since
            # that plenty
            last if ($count > 50);
        }

        close(CITIES);
    }

    # sort the cities so they are in size order (in case multiple tiles were
    # read)
    my @sortedCities = sort sortOnCitySize  @cities;

    return \@sortedCities;
}

##############################################################################
# Name: readLines
#
# Description: Read the lines from a map layer file
#
# Inputs:
#   ulLat - upper-left corner latitude of the minbox of the covered area
#   ulLong - upper-left corner longitude of the minbox of the covered area
#   lrLat - lower-right corner latitude of the minbox of the covered area
#   lrLong - lower-right corner longitude of the minbox of the covered area
#   layerName - the name of the layer currently being read
#
# Returns:
#   A reference to the array of lines read that fall in the requested area.
#   Each entry in the array is a reference to an array of lat/long points.
#
##############################################################################
sub readLines($$$$$)
{
    my $ulLat = $_[0];
    my $ulLong = $_[1];
    my $lrLat = $_[2];
    my $lrLong = $_[3];
    my $layerName = $_[4];

    # determine the tiles for the requested area
    my $tileNames = get15DegreeTiles($ulLat, $ulLong, $lrLat, $lrLong);

    my @lines = ();

    # read the file for each tile
    foreach my $tileName (@$tileNames)
    {
        # build the filename
        my $fileName = "linework/" . $tileName . $layerName . ".1000";

        # open the file.  If the open fails, assume the file doesn't exist
        # and just continue with the next file.
        my $status = open(TILE, "<$fileName");
        next if (!$status);

        # read the data for the current line from the file
        my $count = 0;
        my $error = 0;
        while(my $line = <TILE>)
        {
            # read the name of the line from the file
            chomp($line);
            my $name = $line;

            # read the number of polygons or points for the current polygon
            $line = <TILE>;
            last if (!$line);
            chomp($line);

            my $polygons = 1;
            my $numPoints = $line;
            if ($line < 0)
            {
                # number read is negative, so it is a count of separate
                # polygons for this named entry
                $polygons = -$line;

                # get the number of points in the first polygon
                $line = <TILE>;
                last if (!$line);
                chomp($line);
                $numPoints = $line;
            }

            # read all the polygons from the file
            for (my $poly = 0; $poly < $polygons; $poly++)
            {
                # starting with the second polygon, read the number of points
                # in the polygon (already read for the first polygon)
                if ($poly > 0)
                {
                    $line = <TILE>;
                    last if (!$line);
                    chomp($line);
                    $numPoints = $line;
                }

                # read all the points for this polygon
                my @points = ();
                my $keep = 0;
                for (my $i = 0; $i < $numPoints; $i++)
                {
                    $line = <TILE>;
                    last if (!$line);
                    chomp($line);

                    # get the lat/long for the point
                    my @data = split(' ', $line);

                    # if the wrong number of values were read, something is
                    # wrong with the file, so stop reading it
                    if ($#data != 1)
                    {
                        $error = 1;
                        last;
                    }

                    # convert the point read into a decimal lat/long
                    my $lat = $data[0]/100000.0;
                    my $lon = $data[1]/100000.0;

                    # save the point
                    push(@points, [$lat, $lon]);

                    # track whether the current polygon intersects the visible
                    # area
                    if (!$keep && ($lat <= $ulLat) && ($lat >= $lrLat)
                        && ($lon > $ulLong) && ($lon < $lrLong))
                    {
                        $keep = 1;
                    }
                }

                # if an error occurred, stop reading the file
                last if ($error);

                # add the line to the array to return if it intersects the 
                # visible area
                push(@lines, \@points) if ($keep);
            }

            # if an error occurred, stop reading the file
            last if ($error);
        }

        close(TILE);
    }

    # return the reference to the lines read
    return \@lines;
}

##############################################################################
# Name: drawLines
#
# Description: Draws an array of lines onto an image in the requested color.
#
# Inputs:
#   image - reference to the image to draw the lines on
#   lineArray - reference to the array of lines (lat/long) to draw on the image
#   shadowColor - color to use as a shadow under the actual line
#   lineColor - color to use for the line drawn
#   lineCoefs - equation coefficients to convert lat/long to a line in the image
#   sampleCoefs - equation coefficients to convert lat/long to a sample in the
#                 image
#
# Returns:
#   nothing
#
##############################################################################
sub drawLines($$$$$$)
{
    my $image = $_[0];
    my $lineArray = $_[1];
    my $shadowColor = $_[2];
    my $lineColor = $_[3];
    my $lineCoefs = $_[4];
    my $sampleCoefs = $_[5];

    # loop through each of the lines in the array
    foreach my $points (@$lineArray)
    {
        my $polyline = new GD::Polyline;

        # loop through each of the points in the line
        foreach my $point (@$points)
        {
            my $lat = $$point[0];
            my $lon = $$point[1];

            # convert the lat/long to a line/sample
            my $line = $$lineCoefs[0] + $$lineCoefs[1] * $lon 
                 + $$lineCoefs[2] * $lat + $$lineCoefs[3] * $lat * $lon;
            my $sample = $$sampleCoefs[0] + $$sampleCoefs[1] * $lon 
                 + $$sampleCoefs[2] * $lat + $$sampleCoefs[3] * $lat * $lon;

            # add the point to the line to draw
            $polyline->addPt($sample, $line);
        }

        # draw a shadow for the line
        $image->polydraw($polyline, $shadowColor);

        # draw the line in the requested color, offset from the shadow
        $polyline->offset(1, -1);
        $image->polydraw($polyline, $lineColor);
    }
}

##############################################################################
# Name: drawImage
#
# Description: Draws the output browse image with the requested map layers.
#
# Inputs:
#   imageFilename - name of the original browse image file
#   cornerLats - latitudes of the four corners of the image (UL, UR, LL, LR)
#   cornerLongs - longitudes of the four corners of the image (UL, UR, LL, LR)
#
# Returns:
#
##############################################################################
sub drawImage
{
    my $imageFilename = $_[0];
    my $cornerLats = $_[1];
    my $cornerLongs = $_[2];

    # flag whether the corners are valid
    my $validCorners = 0;
    $validCorners = 1 if (defined $cornerLats && defined $cornerLongs);

    # get the optional width and height parameters
    my $req_width = param('width');
    my $req_height = param('height');

    # get the optional parameters for which map layers to include
    my $drawCities = param('cities');
    my $drawCounties = param('counties');
    my $drawAdmin = param('admin');

    # get a overall indication of whether map layers are going to be drawn
    my $drawMapLayers = 0;
    if ($validCorners && ((defined $drawCities) || (defined $drawCounties)
            || (defined $drawAdmin)))
    {
        $drawMapLayers = 1;
    }

    # read the input image
    my $status = open(FILE,"<$imageFilename");
    handleerror() if (!$status);
    my $image = GD::Image->newFromJpeg(\*FILE);
    close FILE;

    # get the image size
    my ($width,$height) = $image->getBounds();

    # default the requested width and height if they are not defined
    $req_width = $width if (not defined $req_width);
    $req_height = $height if (not defined $req_height);

    # limit the width and height to a reasonable size
    $req_width = 1000 if ($req_width > 1000);
    $req_height = 1000 if ($req_height > 1000);
    $req_width = 100 if ($req_width < 100);
    $req_height = 100 if ($req_height < 100);

    # create the output image
    my $outputImage = GD::Image->new($req_width, $req_height);

    # allocate the colors for the map layers
    my $countyColor;
    my $adminColor;
    my $cityNameColor;
    my $shadowColor;
    if ($drawMapLayers)
    {
        $countyColor = $outputImage->colorAllocate(255, 255, 255);
        $adminColor = $outputImage->colorAllocate(0, 0, 0);
        $cityNameColor = $outputImage->colorAllocate(255, 255, 0);
        $shadowColor = $outputImage->colorAllocate(0, 0, 0);
    }

    # copy the source image to the output image, scaling to the requested size
    $outputImage->copyResized($image, 0, 0, 0, 0, $req_width, $req_height,
                              $width, $height);

    # make output lines 2 pixels wide
    $outputImage->setThickness(2);

    # if valid corners are available for the image, draw the requested map
    # layers
    if ($drawMapLayers)
    {
        # calculate the equation coefficients to convert a lat/long into a 
        # line/sample in the image
        my @lines = (0, 0, $req_height, $req_height);
        my @samples = (0, $req_width, 0, $req_width);
        my @lineCoefs;
        my @sampleCoefs;
        calcLineSampleCoefs($cornerLats, $cornerLongs, \@lines, \@samples,
                         \@lineCoefs, \@sampleCoefs);

        # calculate the corners for the lat/long box covering the area of 
        # interest (as a quick filter when reading map layers)
        my $ulLat = $$cornerLats[0];
        my $ulLong = $$cornerLongs[0];
        my $lrLat = $$cornerLats[0];
        my $lrLong = $$cornerLongs[0];
        for (my $i = 1; $i < 4; $i++)
        {
            $ulLat = $$cornerLats[$i] if ($$cornerLats[$i] > $ulLat);
            $ulLong = $$cornerLongs[$i] if ($$cornerLongs[$i] < $ulLong);
            $lrLat = $$cornerLats[$i] if ($$cornerLats[$i] < $lrLat);
            $lrLong = $$cornerLongs[$i] if ($$cornerLongs[$i] > $lrLong);
        }

        # if the counties were requested, draw them on the image
        if ($drawCounties)
        {
            # read the counties map information for this area
            my $counties = readLines($ulLat, $ulLong, $lrLat, $lrLong,
                                     "Counties");

            # draw the returned county lines
            drawLines($outputImage, $counties, $shadowColor, $countyColor,
                      \@lineCoefs, \@sampleCoefs);
            undef $counties;
        }

        # if the admin boundaries were requested, draw them on the image
        if ($drawAdmin)
        {
            # read the world admin map information for this area
            my $admin = readLines($ulLat, $ulLong, $lrLat, $lrLong, "Admin");

            # draw the admin boundaries
            drawLines($outputImage, $admin, $shadowColor, $adminColor,
                      \@lineCoefs, \@sampleCoefs);
            undef $admin;
        }

        # if the cities boundaries were requested, draw them on the image
        if ($drawCities)
        {
            # read the cities map information for this area
            my $cities = readCities($ulLat, $ulLong, $lrLat, $lrLong);

            # get the giant font for drawing names
            my $font = GD::Font->Giant;

            my $fontWidth = $font->width;
            my $fontHeight = $font->height;
            my $heightAdjust = -$fontHeight/2;

            my @drawn = ();

            # draw the returned cities
            my $count = 0;
            foreach my $city (@$cities)
            {
                my $lat = $$city[1];
                my $lon = $$city[2];
                my $name = $$city[3];

                my $line = $lineCoefs[0] + $lineCoefs[1] * $lon 
                     + $lineCoefs[2] * $lat + $lineCoefs[3] * $lat * $lon;
                my $sample = $sampleCoefs[0] + $sampleCoefs[1] * $lon 
                     + $sampleCoefs[2] * $lat + $sampleCoefs[3] * $lat * $lon;

                $line -= $heightAdjust;
                $sample -= length($name) * $fontWidth / 2.0;
                my $end_sample = $sample + length($name) * $fontWidth;
                my $end_line = $line + $fontHeight;

                # if the name isn't completely in the image with some buffer,
                # skip it
                next if ($line < 10);
                next if ($sample < 10);
                next if ($end_sample > ($req_width - 10));
                next if ($end_line > ($req_height - 10));

                # make sure this city is sufficiently far enough away from one
                # that has already been drawn
                my $too_close = 0;
                foreach my $box (@drawn)
                {
                    my $sl = $$box[0];
                    my $ss = $$box[1];
                    my $el = $$box[2];
                    my $es = $$box[3];

                    if ((abs($sl - $line) < 60) 
                         && ((($sample > $ss) && ($sample < $es))
                             || (($end_sample > $ss) && ($end_sample < $es))))
                    {
                        $too_close = 1;
                        last;
                    }
                }
                next if ($too_close);

                # save the bounding box of the names that are drawn, with a
                # little extra cushion on the edges
                push(@drawn, [$line, $sample - 60, $end_line,
                              $end_sample + 60]);

                # draw the name twice, once as an offset shadow and once using
                # the real color to make it more readable
                $outputImage->string($font, $sample-1, $line+1, $name,
                                     $shadowColor);
                $outputImage->string($font, $sample, $line, $name,
                                     $cityNameColor);

                # only draw 5 names on the image
                $count++;
                last if ($count > 4);
            }
        }
    }

    # write the final image
    print "Content-type: image/jpeg\n\n";
    print ($outputImage->jpeg(75));
}

##############################################################################
# Name: handleerror
#
# Description: This routine is called when an error is encountered.  It builds
#   a black image with a red 'X' through it to indicate the real image could
#   not be built.
#
# Inputs:
#   none
#
# Returns:
#   does not return - exits the script
#
##############################################################################
# output an error image
sub handleerror
{
    my $width = param('width');
    my $height = param('height');
    $width = 100 if (not defined $width);
    $height = 100 if (not defined $height);
    $width = 1000 if ($width > 1000);
    $height = 1000 if ($height > 1000);
    $width = 100 if ($width < 100);
    $height = 100 if ($height < 100);

    my $outputImage = GD::Image->new($width, $height);
    my $blackColor = $outputImage->colorAllocate(0, 0, 0);
    my $redColor = $outputImage->colorAllocate(255, 0, 0);
    $outputImage->fill(0, 0, $blackColor);
    $outputImage->setThickness(2);
    $outputImage->line(0, 0, $width-1, $height-1, $redColor);
    $outputImage->line($width-1, 0, 0, $height-1, $redColor);

    print "Content-type: image/jpeg\n\n";
    print ($outputImage->jpeg(75));
    exit;
}

##############################################################################
# Name: readMetaFile
#
# Description: Reads the indicated metadata file to obtain the browse image
#   name and the image corners.
#
# Inputs:
#   metafile - name of the metadata file
#
# Returns:
#
##############################################################################
# routine to read the local browse file name from the metadata file
sub readMetaFile
{
    my $metafile = $_[0];

    my @cornerLats = ();
    my @cornerLongs = ();
    my $localBrowse = "";
    
    # open the metadata file or output an error if it fails
    open (META, "<".$metafile) or handleerror();

    # read the metadata file to get the browse name and the corners
    my $line;
    while ($line = <META>)
    {
        chomp $line;
        my($name,$value) = split (/\s*=\s*/,$line);

        if ($name eq "LocalBrowseName")
        {
            $localBrowse = $value;
        }
        elsif ($name eq "corner_ul_lat")
        {
            $cornerLats[0] = $value;
        }
        elsif ($name eq "corner_ur_lat")
        {
            $cornerLats[1] = $value;
        }
        elsif ($name eq "corner_ll_lat")
        {
            $cornerLats[2] = $value;
        }
        elsif ($name eq "corner_lr_lat")
        {
            $cornerLats[3] = $value;
        }
        elsif ($name eq "corner_ul_lon")
        {
            $cornerLongs[0] = $value;
        }
        elsif ($name eq "corner_ur_lon")
        {
            $cornerLongs[1] = $value;
        }
        elsif ($name eq "corner_ll_lon")
        {
            $cornerLongs[2] = $value;
        }
        elsif ($name eq "corner_lr_lon")
        {
            $cornerLongs[3] = $value;
        }
    }
    close(META);

    # verify all the corners were read
    for (my $i = 0; $i < 4; $i++)
    {
        if ((not defined $cornerLats[$i]) or (not defined $cornerLongs[$i]))
        {
            # not all corners were read, so just return the browse file name
            return $localBrowse;
        }
    }

    # all the corners were read, so return everything
    return $localBrowse, \@cornerLats, \@cornerLongs;
}


#!/usr/bin/perl -wT
##############################################################################
# Name: getBrowse.cgi
#
# Description: This cgi script searches for the browse image closest to the 
#   latitude, longitude, date and sensor passed in.  A URL to the located 
#   browse image is returned, along with the dates of the previous and next
#   scenes.
#
# Input Parameters:
#   Either
#       lat - latitude of the desired browse
#       lon - longitude of the desired browse
#   Or
#       path - WRS-2 of the desired browse
#       row - WRS-2 row of the desired browse
#   date - date of the desired browse (may be omitted and the most recent, 
#          lowest cloud cover scene will be located)
#   sensor - the dataset to search for the browse.  It can be LANDSAT_8_OLI,
#            LANDSAT_ETM, LANDSAT_ETM_SLC_OFF, or LANDSAT_TM.
#            If the sensor is omitted, the search will default to
#            LANDSAT_COMBINED over the US or the LANDSAT_COMBINED dataset
#            containing the target date if not over the US.
#
# Returns:
#   outputs data to stdout in xml format
##############################################################################

# use strict mode
use strict;

# add the directory for utility modules
use lib "perllib";

# use the CGI module for getting parameters and creating html output
use CGI qw(:standard escapeHTML);

# use the Date::Manip module for calculating the differences between dates
use Date::Manip;

# use the Scalar::Util module for checking for valid numbers
use Scalar::Util qw(looks_like_number);

# use the ll2PathRow module for converting lat/long to path/row
use ll2PathRow;
# use the PathRow2ll module for converting path/row to lat/long
use PathRow2ll;

# output the xml mime content type
print "Content-type: text/xml\n\n";

# output the xml header
print "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
print "<glovisResponse version=\"1.0\">\n";

# get the latitude, longitude, date, and sensor passed in
my $tainted_lat = param('lat');
my $tainted_lon = param('lon');
my $tainted_date = param('date');
my $tainted_sensor = param('sensor');
my $tainted_path = param('path');
my $tainted_row = param('row');

# if a date is not provided, set the flag to search for the scene to display
$tainted_date = "search" if (not defined $tainted_date);

# default the sensor to Landsat combined dataset if it isn't provided
$tainted_sensor = "LANDSAT_COMBINED" if (not defined $tainted_sensor);

# default to using the lat/lon parameters
my $use_lat_lon = 1;
my $lat;
my $lon;
my $path;
my $row;

# flag to indicate path/row parameters were ignored
my $ignored_path_row = 0;

# if the lat/lon parameters were provided, use them.  Otherwise if the path/row
# were entered use them.
if ((not defined $tainted_lat) && (not defined $tainted_lon) 
    && (defined $tainted_path) && (defined $tainted_row))
{
    # lat/lon parameters were not provided, but path/row were, so use them
    $use_lat_lon = 0;

    # untaint the path/row parameters
    handle_error("path parameter is invalid") 
        unless($tainted_path =~ m#^([\w.-]+)$#
            && looks_like_number($tainted_path));
    $path = $1;
    handle_error("path parameter value of $path is outside the 1 to 233 range")
        if (($path < 1) || ($path > 233));

    handle_error("row parameter is invalid") 
        unless($tainted_row =~ m#^([\w.-]+)$#
            && looks_like_number($tainted_row));
    $row = $1;
    handle_error("row parameter value of $row is outside the 1 to 124 range")
        if (($row < 1) || ($row > 124));
}
elsif ((defined $tainted_lat) && (defined $tainted_lon))
{
    # untaint the lat/lon parameters
    handle_error("lat parameter is invalid") 
        unless($tainted_lat =~ m#^([\w.-]+)$#
            && looks_like_number($tainted_lat));
    $lat = $1;
    handle_error("lat parameter value of $lat is outside the -89 to 89 range")
        if (($lat < -89) || ($lat > 89));

    handle_error("lon parameter is invalid") 
        unless($tainted_lon =~ m#^([\w.-]+)$#
            && looks_like_number($tainted_lon));
    $lon = $1;
    handle_error("lon parameter value of $lon is outside the -180 to 180 range")
        if (($lon < -180) || ($lon > 180));

    # if the path/row parameters were defined, flag that they were ignored
    $ignored_path_row = 1 
        if ((defined $tainted_path) || (defined $tainted_row));
}
else
{
    handle_error("lat and lon OR path and row required");
}

# untaint the date and sensor parameters
handle_error("date parameter is invalid") 
    unless($tainted_date =~ m#^([\w.-]+)$#);
my $target_date = lc($1);

handle_error("sensor parameter is invalid") 
    unless($tainted_sensor =~ m#^([\w.-]+)$#);
my $sensor = uc($1);

# verify the target_date is either "search" or 8 digits
if (($target_date ne "search") && !($target_date =~ /\d{8,8}/))
{
    handle_error("date format not yyyymmdd");
}

if ($use_lat_lon == 1)
{
    # find the path/row that matches the provided lat/long
    ($path, $row) = lat_long_to_path_row("WRS2", $lat, $lon, 0);
    handle_error("path/row could not be located from lat/long $lat/$lon") 
        if (not defined $path);
}
else
{
    # find the lat/lon that matches the provided path/row
    ($lat, $lon) = path_row_to_lat_long("WRS2", $path, $row);
}

# read the TOC file (or files)
my @toc_contents = read_toc_files($sensor, $path, $row);

# find the scene id closest to the provided target_date
my $scene_id; 
my $prev_date;
my $next_date; 
my $metadata_file_name;
my %metaInfo;
my $browse_file;
if ($#toc_contents >= 0)
{
    ($scene_id, $prev_date, $next_date) 
            = findSceneInToc($target_date, @toc_contents);
    if (defined $scene_id)
    {
        my $dataset;

        # get the metadata file name and dataset name for the scene id
        ($metadata_file_name, $dataset) = get_metadata_file_name($scene_id);

        # read the contents of the metadata file
        %metaInfo = read_metadata_file($metadata_file_name);
        $metaInfo{dataset} = $dataset;

        # get the browse file location indicated by the metadata
        $browse_file = get_browse_file_name($metadata_file_name, \%metaInfo);
    }
}

if (defined $browse_file)
{
    # include a warning that path/row were ignored, eventhough things were
    # successful
    my $extra_msg = "";
    $extra_msg = ", extra path/row parameters ignored" 
        if ($ignored_path_row == 1);
    # got to this point, so we can successfully output the requested info
    print "\n    <returnStatus value=\"success\">Completed successfully".
          "$extra_msg</returnStatus>\n\n";

    # build the url for browse file and output the xml data
    my $browse_url = url();
    $browse_url =~ s/getBrowse.cgi/getBrowseImage.cgi/;
    $browse_url .= "?scene_id=$scene_id";
    print "    <!-- link to the primary browse image -->\n\n";
    print "    <image>\n";
    print "        <url>$browse_url</url>\n";
    print "    </image>\n\n";

    # output the link to the overview map image
    output_overview_map_record($metaInfo{scene_center_lat},
                               $metaInfo{scene_center_lon});

    # output the link to the FGDC metadata
    my $collection_id = '';
    if ($metaInfo{dataset} eq "LANDSAT_8_OLI")
    {
        $collection_id = '4601';
    }
    elsif ($metaInfo{dataset} eq "LANDSAT_ETM_SLC_OFF")
    {
        $collection_id = '3373';
    }
    elsif ($metaInfo{dataset} eq "LANDSAT_ETM")
    {
        $collection_id = '3372';
    }
    elsif ($metaInfo{dataset} eq "LANDSAT_TM")
    {
        $collection_id = '3119';
    }
    else
    {
        handle_error("dataset value is invalid");
    }
    my $fgdc_server 
      = "http://earthexplorer.usgs.gov/form/fgdcmetadatalookup";
    print "    <!-- link to associated FGDC metadata for primary browse image "
          . "-->\n\n";
    print "    <FGDCMetadata>\n";
    print "        <url>$fgdc_server?collection_id=" . $collection_id .
          "&amp;entity_id=" . $metaInfo{entityID} .
          "&amp;primary_key=" . $metaInfo{entityID} .
          "&amp;pageView=1</url>\n";
    print "    </FGDCMetadata>\n\n";

    # output the metadata for the scene
    output_metadata(\%metaInfo);

    # output the link to the previous date image
    print "\n    <!-- link to previous date browse image -->\n\n";
    if ($prev_date ne "unknown")
    {
        output_browse_record("prevDateImage", $lat, $lon, $path, $row, 
                             $use_lat_lon, $sensor, $prev_date);
    }
    else
    {
        output_browse_record("prevDateImage", undef, undef, undef, undef,
                             undef, undef, undef);
    }

    # output the link to the next date image
    print "\n    <!-- link to next date browse image -->\n\n";
    if ($next_date ne "unknown")
    {
        output_browse_record("nextDateImage", $lat, $lon, $path, $row, 
                             $use_lat_lon, $sensor, $next_date);
    }
    else
    {
        output_browse_record("nextDateImage", undef, undef, undef, undef,
                             undef, undef, undef);
    }
}
else
{
    print "\n    <returnStatus value=\"error\">No browse available matching " .
          "parameters</returnStatus>\n";

    # output overview map url
    output_overview_map_record($lat, $lon);
}

my @adjacent_lat_long = get_adjacent_cells_lat_long($path, $row);
my %pan_tags = (UL => "upperLeftScene",
                UC => "upperCenterScene",
                UR => "upperRightScene",
                CL => "centerLeftScene",
                CR => "centerRightScene",
                LL => "lowerLeftScene",
                LC => "lowerCenterScene",
                LR => "lowerRightScene");

print "\n    <!-- secondary query info for \"pan\" capability -->\n\n";
foreach my $record (@adjacent_lat_long)
{
    my $tag = $pan_tags{$record->{dir}};
    next if (not defined $tag);
    my $lat = $record->{lat};
    my $lon = $record->{lon};
    my $path = $record->{path};
    my $row = $record->{row};
    output_browse_record($tag, $lat, $lon, $path, $row, $use_lat_lon, $sensor,
                         $target_date);
    print "\n";
}

print "</glovisResponse>\n";

exit;

##############################################################################
# Name: output_browse_record
#
# Description: outputs an record in xml format that contains the parameters to
#   call this script to load a scene
#
# Inputs:
#   scene tag - browse record name (i.e. nextDateImage, upperLeftScene, etc)
#   latitude - latitude of the scene center
#   longitude - longitude of the scene center
#   path - WRS path of the scene center
#   row - WRS row of the scene center
#   sensor - sensor name
#   date - date of the scene
#
# Returns:
#   nothing
##############################################################################
sub output_browse_record
{
    my ($scene_tag, $lat, $lon, $path, $row, $use_lat_lon, $sensor, $date) = @_;

    if (defined $lat)
    {
        print "    <$scene_tag nParams=\"4\" queryType=\"getBrowse.cgi\">\n";
        if ($use_lat_lon == 1)
        {
            print "        <queryParam name=\"lat\">$lat</queryParam>\n";
            print "        <queryParam name=\"lon\">$lon</queryParam>\n";
        }
        else
        {
            print "        <queryParam name=\"path\">$path</queryParam>\n";
            print "        <queryParam name=\"row\">$row</queryParam>\n";
        }
        print "        <queryParam name=\"sensor\">$sensor</queryParam>\n";
        print "        <queryParam name=\"date\">$date</queryParam>\n";
    }
    else
    {
        print "    <$scene_tag nParams=\"0\" queryType=\"getBrowse.cgi\">\n";
    }
    print "    </$scene_tag>\n";
}

##############################################################################
# Name: output_overview_map_record
#
# Description: outputs an record in xml format that contains the parameters to
#   call the getOverviewMap cgi script for the current location
#
# Inputs:
#   latitude - latitude of the scene center
#   longitude - longitude of the scene center
#
# Returns:
#   nothing
##############################################################################
sub output_overview_map_record
{
    my ($lat, $lon) = @_;

    my $browse_url = url();
    $browse_url =~ s/getBrowse.cgi//;
    my $overview_map_url = $browse_url . "getOverviewMap.cgi?lat=" . $lat 
            . "&amp;lon=" . $lon;

    print "    <!-- link to the overview map of the browse location -->\n\n";
    print "    <overviewMap>\n";
    print "        <url>$overview_map_url</url>\n";
    print "    </overviewMap>\n\n";
}

##############################################################################
# Name: get_adjacent_cells_lat_long
#
# Description: find the cells adjacent to the path/row passed in and return
#   the lat/long of those cell scene centers.
#
# Inputs:
#   path - WRS-2 path of center cell
#   row - WRS-2 row of center cell
#
# Returns:
#   array of references to a hash record.  The hash record holds the direction
#   for the cell, the latitude of the cell center, and the longitude of the
#   cell center.
##############################################################################
sub get_adjacent_cells_lat_long
{
    my ($center_path, $center_row) = @_;

    my @path_dir = ("R", "C", "L");
    my @row_dir = ("U", "C", "L");

    my @cells = ();
    for (my $row_adj = 0; $row_adj <= 2; $row_adj++)
    {
        my $row = $center_row + $row_adj - 1;

        for (my $path_adj = 2; $path_adj >= 0; $path_adj--)
        {
            my $path = $center_path + $path_adj - 1;
            $path = 1 if ($path == 234);
            $path = 233 if ($path == 0);

            next if (($path == $center_path) && ($row == $center_row));

            my $lat;
            my $lon;

            # if the row is outside the normal area, leave the lat/lon undefined
            if (($row >= 1) && ($row <= 124))
            {
                # convert the path/row to a lat/long
                ($lat, $lon) = path_row_to_lat_long("WRS2", $path, $row);

                # limit the decimal places reported for lat/long
                $lat = sprintf("%7.4f", $lat);
                $lon = sprintf("%8.4f", $lon);
            }

            my %record = (dir => $row_dir[$row_adj] . $path_dir[$path_adj],
                          lat => $lat, lon => $lon, path => $path, row => $row);
            push (@cells, \%record);
        }
    }

    return @cells;
}

##############################################################################
# Name: handle_error
#
# Description: routine to output an error indication in the xml output
#
# Inputs:
#   error message (if illegal values, do not pass them here, but if a value
#       was legal but invalid (out of range) the value may be printed)
##############################################################################
sub handle_error
{
    my $error_message = $_[0];

    print "\n    <returnStatus value=\"error\">$error_message</returnStatus>\n";
    print "\n</glovisResponse>\n";
    exit;
}

##############################################################################
# Name: read_toc_files
#
# Description: read the entries in the TOC file or files for the sensor, path,
#   and row.  If the sensor is "LANDSAT_COMBINED", the TOC files for all the
#   supported sensors will be read.
#
# Inputs:
#   sensor - sensor name (LANDSAT_8_OLI, LANDSAT_ETM, LANDSAT_ETM_SLC_OFF,
#            LANDSAT_TM, or LANDSAT_COMBINED)
#   path - WRS-2 path to read
#   row - WRS-2 row to read
#
# Returns:
#   array of lines read from the TOC file or files, sorted from oldest to
#   newest dates 
##############################################################################
sub read_toc_files
{
    my ($sensor, $path, $row) = @_;

    # build the list of sensor directories to read.  For LANDSAT_COMBINED, 
    # read the TOCs from L8 OLI, L7 ETM+, L4-5 TM, and L7 SLC-off
    my @sensor_dirs;
    if ($sensor eq "LANDSAT_COMBINED")
    {
        @sensor_dirs = ("l8oli", "l7", "l5", "l7slc_off");
    }
    elsif ($sensor eq "LANDSAT_8_OLI")
    {
        @sensor_dirs = ("l8oli");
    }
    elsif ($sensor eq "LANDSAT_ETM")
    {
        @sensor_dirs = ("l7");
    }
    elsif ($sensor eq "LANDSAT_TM")
    {
        @sensor_dirs = ("l5");
    }
    elsif ($sensor eq "LANDSAT_ETM_SLC_OFF")
    {
        @sensor_dirs = ("l7slc_off");
    }
    else
    {
        handle_error("sensor parameter $sensor is invalid");
    }

    # read the contents of the TOC files into the toc_contents array
    my @toc_contents;
    foreach my $sensor_dir (@sensor_dirs)
    {
        # build the TOC file name for this directory
        my $cell_dir = $sensor_dir . "/p" . sprintf("%03d", $path) . "/r" 
                        . sprintf("%03d", $row);
        my $toc_file = $cell_dir . "/TOC";

        # open the TOC file, moving to the next file if the TOC open fails
        # since the TOC may not exist if no scenes are in the cell
        my $status = open(TOC, "<$toc_file");
        next if (!$status);

        # read the TOC file, skipping the first line since it is a header line
        my $first_line = 1;
        while (my $line = <TOC>)
        {
            # skip the first line of the TOC file since it is a header line that
            # doesn't contain any scenes
            if ($first_line == 1)
            {
                # skip header line
                $first_line = 0;
                next;
            }

            chomp($line);

            # save the line
            push(@toc_contents, $line);
        }
        close (TOC);
    }

    # sort the toc contents by date.  The sort uses the default alphabetical 
    # order sort, which is okay since the date is the first thing on each line
    # of the TOC file and sorting that alphabetically is the same as sorting it
    # by date.
    @toc_contents = sort(@toc_contents);

    return @toc_contents;
}

##############################################################################
# Name: findSceneInToc
#
# Description: finds the scene that most closely matches the input parameters.
#
# Inputs:
#   target date - target date for the desired scene.  "search" if no target
#                 date was provided (in which case, the most recent, lowest
#                 cloud cover scene will be returned).  If the target date
#                 is not reasonable, the script exits with an error.
#   TOC contents - the contents of the TOC file (or files) that have been read
#                  into memory and sorted by date.
#
# Returns:
#   array containing the following:
#       scene id of the scene found (undef if no matching scene found)
#       date of the scene in the inventory before the scene found (unknown if
#           no previous scene in the inventory)
#       date of the scene in the inventory after the scene found (unknown if
#           no following scene in the inventory)
##############################################################################
sub findSceneInToc
{
    my ($target_date, @toc_contents) = @_;

    my $scene_id;

    # convert the target date into the days since 1BC
    my $target_days;
    if ($target_date ne "search")
    {
        # initialize the Date::Manip module
        Date_Init();

        # extract the year, month, and day from the target date
        my $target_year = substr($target_date, 0, 4);
        my $target_month = substr($target_date, 4, 2);
        my $target_day = substr($target_date, 6, 2);

        # if the date isn't somewhat reasonable, generate an error
        if (($target_year < 1970) || ($target_year > 2100)
            || ($target_month < 1) || ($target_month > 12)
            || ($target_day < 1) || ($target_day > 31))
        {
            handle_error("$target_date not a legal date");
        }

        # calculate the target days
        $target_days 
            = Date_DaysSince1BC($target_month, $target_day, $target_year);
    }

    # initialize the variables used to track the search
    my $prev_date = "unknown";
    my $found_prev_date = "unknown";
    my $found_next_date = "unknown";
    my $get_next_date = 0;
    my $min_days_diff = 1000000;
    my $maxCC = 101;
    my $first_line = 1;

    for (my $i = 0; $i <= $#toc_contents; $i++)
    {
        # split the TOC line into separate fields
        my @fields = split(',', $toc_contents[$i]);

        # if looking for the next date, set it to the date from this line
        if ($get_next_date)
        {
            $found_next_date = $fields[0];
            $get_next_date = 0;
        }

        # if looking for a specific date, parse the date for this TOC file line
        if ($target_date ne "search")
        {
            my $year = substr($fields[0], 0, 4);
            my $month = substr($fields[0], 4, 2);
            my $day = substr($fields[0], 6, 2);
            my $days = Date_DaysSince1BC($month, $day, $year);

            # if this date is the closest to the target date, make it the new
            # "found" scene
            my $diff_days = abs($target_days - $days);
            if ($diff_days <= $min_days_diff)
            {
                $min_days_diff = $diff_days;
                $scene_id = $fields[5];
                # remember the previous scene's date
                $found_prev_date = $prev_date;
                # get the next scene's date as the next date
                $found_next_date = "unknown";
                $get_next_date = 1;
            }
        }
        else
        {
            # not searching for a target date, so find the most recent, lowest
            # cloud cover scene
            my $cc = $fields[4];
            if ($cc <= $maxCC)
            {
                $maxCC = $cc;
                $scene_id = $fields[5];
                # remember the previous scene's date
                $found_prev_date = $prev_date;
                # get the next scene's date as the next date
                $found_next_date = "unknown";
                $get_next_date = 1;
            }
        }
        # remember the current date as the previous date so it can be used
        # if the correct scene is found
        $prev_date = $fields[0];
    }

    return $scene_id, $found_prev_date, $found_next_date;
}

##############################################################################
# Name: read_metadata_file
#
# Description: routine to read the contents of the metadata file and populate
#   a hash with the contents
#
# Inputs:
#   metadata file name - full path name of the metadata file to read
#
# Returns:
#   the hash with the metadata file contents (key of the hash is the tag name)
##############################################################################
sub read_metadata_file
{
    my($metafile) = @_;

    open (META, "<".$metafile) or handle_error("error reading metadata file");

    my %metadata;
    while (my $line = <META>)
    {
        chomp($line);
        next if ($line =~ /^\s*$/); # Skip any blank lines
        # There are no spaces in keys, so all non-space before the = is the key
        # and everything after the = (and optional whitespace) is the value
        if ($line =~ /(\S+)\s*=\s*(.*)/)
        {
            $metadata{$1} = $2; # $2 could be empty
        }
    }
    close(META);

    return %metadata;
}

##############################################################################
# Name: get_metadata_file_name
#
# Description: finds the metadata file name for the provided scene id
#
# Inputs:
#   scene id - scene id to find the browse file name for
#
# Returns:
#   full path to the metadata file
#   dataset name (LANDSAT_TM, LANDSAT_ETM, LANDSAT_ETM_SLC_OFF, LANDSAT_8_OLI)
##############################################################################
sub get_metadata_file_name
{
    my ($scene_id) = @_;
    my $dataset;

    # determine the directory for the scene by checking the sensor number in
    # the scene id
    my $sensor_number = substr($scene_id, 2, 1);
    my $sensor_dir;
    if (($sensor_number == 4) || ($sensor_number == 5))
    {
        $sensor_dir = "l5";
        $sensor_number = 5;
        $dataset = "LANDSAT_TM";
    }
    elsif ($sensor_number == 7)
    {
        # for now, assume the normal Landsat ETM+ directory.  It will be 
        # determined later if the SLC-off directory should be used instead.
        $sensor_dir = "l7";
        $dataset = "LANDSAT_ETM";
    }
    elsif ($sensor_number == 8)
    {
        $sensor_dir = "l8oli";
        $dataset = "LANDSAT_8_OLI";
    }
    else
    {
        handle_error("illegal scene id");
    }

    # extract the path and row from the scene id
    my $path = substr($scene_id,3,3);
    my $row = substr($scene_id,6,3);

    # build the cell directory name
    my $cell_dir = $sensor_dir . "/p" . sprintf("%03d", $path) . "/r" 
                    . sprintf("%03d", $row);

    # extract the year from the scene id
    my $scene_year = substr($scene_id, 9, 4);

    # build the metadata file name since the browse name needs to be extracted
    # from the metadata file (since the browse file names don't necessarily
    # follow a predictable pattern)
    my $year_dir = $cell_dir . "/y" . $scene_year;
    my $meta_file = $year_dir . "/" . $scene_id . ".meta";

    # if the metadata file doesn't exist and the sensor is Landsat 7, change
    # to the SLC-off directory
    if ((! -e $meta_file) && ($sensor_number == 7))
    {
        $meta_file =~ s/^l7\//l7slc_off\//;
        $dataset = "LANDSAT_ETM_SLC_OFF";
    }

    return $meta_file, $dataset;
}

##############################################################################
# Name: get_browse_file_name
#
# Description: finds the browse file name for the provided scene id
#
# Inputs:
#   metadata file name - full path to metadata file read
#   metadata reference - reference to the metadata info read
#
# Returns:
#   full path to the browse file
##############################################################################
sub get_browse_file_name
{
    my ($metadata_file_name, $metadata_ref) = @_;

    # get the browse file name from the metadata file
    my $browse_file = $metadata_ref->{LocalBrowseName};

    # get the path to the browse file from the metadata file name
    my $index = rindex($metadata_file_name, "/");
    my $dir = substr($metadata_file_name, 0, $index + 1);

    # add the full path to the browse file name
    $browse_file = $dir . $browse_file if (defined $browse_file);

    return $browse_file;
}

# routine to convert Y/N flags into a full word description
sub Yes_No
{
    $_ = $_[0];
    return "Yes" if (m/^[Y|y]/);
    return "No";
}

# routine to convert D/N flags into a full word description
sub Day_Night
{
    $_ = $_[0];
    return "Day" if (m/^[D|d]/);
    return "Night";
}

# routine to convert A/D flags into a full word description
sub Desc_Ascend
{
    $_ = $_[0];
    return "Descending" if (m/^[D|d]/);
    return "Ascending";
}

# routine to convert H/L flags into a full word description
sub High_Low
{
    $_ = $_[0];
    return "High" if (m/^[H|h]/);
    return "Low";
}

# routine to convert Z2/z2 flags into a full word description
sub Order_Status
{
    $_ = $_[0];
    return "Not Orderable Online" if (m/^Z2/) || (m/^z2/) ;
    return "Orderable Online";
}

# routine to convert Sensor flags into a full word description (used for 4-5)
sub Satellite_Number
{
    $_ = $_[0];
    return "Landsat 1" if (m/^1/);
    return "Landsat 2" if (m/^2/);
    return "Landsat 3" if (m/^3/);
    return "Landsat 4" if (m/^4/);
    return "Landsat 5" if (m/^5/);
    return "Landsat 7" if (m/^7/);
    return "Landsat 8" if (m/^8/);
    return $_;
}

# routine to convert value of 99 into a null value represented by a blank line
sub Filter_SunElevation
{
    $_ = $_[0];
    return "" if (m/^99/);
    return $_;
}
    
##############################################################################
# Name: output_metadata
#
# Description: outputs selected Level 0 metadata in xml format
#
# Inputs:
#   metadata reference - reference to the metadata read from the metadata file
#
# Returns:
#   nothing - outputs metadata to stdout
##############################################################################
sub output_metadata
{
    my ($metaInfo_ref) = @_;
    my $scene_id = $metaInfo_ref->{entityID};

    # assign the values to the metadata hash that is used to populate the table
    my %metadata;
    $metadata{"sceneId"} = $metaInfo_ref->{entityID};
    $metadata{"path"} = $metaInfo_ref->{path};
    $metadata{"row"} = $metaInfo_ref->{row};
    $metadata{"sensor"} = $metaInfo_ref->{dataset};

    $metadata{"sunAzimuth"} = $metaInfo_ref->{sun_azimuth};
    $metadata{"receivingStation"} = $metaInfo_ref->{station_id};
     
    # format the Acquisition Date value
    my $date = substr($metaInfo_ref->{date_acquired},0,4) . "/" .
               substr($metaInfo_ref->{date_acquired},4,2) . "/" .
               substr($metaInfo_ref->{date_acquired},6,2);
    $metadata{"acquisitionDate"} = $date;

    # format ULLat coordinate values
    $metadata{"upperLeftCornerLatitude"} = $metaInfo_ref->{corner_ul_lat};
    $metadata{"upperLeftCornerLongitude"} = $metaInfo_ref->{corner_ul_lon};

    # format URLat coordinate values
    $metadata{"upperRightCornerLatitude"} = $metaInfo_ref->{corner_ur_lat};
    $metadata{"upperRightCornerLongitude"} = $metaInfo_ref->{corner_ur_lon};

    # format LRLong coordinate values
    $metadata{"lowerRightCornerLatitude"} = $metaInfo_ref->{corner_lr_lat};
    $metadata{"lowerRightCornerLongitude"} = $metaInfo_ref->{corner_lr_lon};

    # format LLLong coordinate values
    $metadata{"lowerLeftCornerLatitude"} = $metaInfo_ref->{corner_ll_lat};
    $metadata{"lowerLeftCornerLongitude"} = $metaInfo_ref->{corner_ll_lon};

    # format the Scene Center coordinate values
    $metadata{"sceneCenterLatitude"} = $metaInfo_ref->{scene_center_lat};
    $metadata{"sceneCenterLongitude"} = $metaInfo_ref->{scene_center_lon};

    # format cloud cover value
    $metadata{"cloudCover"} = $metaInfo_ref->{cloud_cover} . "%";
   
    # format Browse Available value
    $metadata{"browseAvailable"} = Yes_No($metaInfo_ref->{browse_exists}); 

    # format sun elevation value
    $metadata{"sunElevation"}
            = Filter_SunElevation($metaInfo_ref->{sun_elevation});

    # format Day or Night value
    $metadata{"dayOrNight"} = Day_Night($metaInfo_ref->{day_night});

    # include information about the bands included in the browse image
    $metadata{"browseRedBandNumber"} = 5;
    $metadata{"browseRedBandType"} = "Short Wavelength Infrared";
    $metadata{"browseGreenBandNumber"} = 4;
    $metadata{"browseGreenBandType"} = "Near Infrared";
    $metadata{"browseBlueBandNumber"} = 3;
    $metadata{"browseBlueBandType"} = "Red";

    # Separating the fields that are needed by sensor to avoid an error
    # of uninitialized value.
    my $sensor = substr($scene_id, 2, 1);
    if (($sensor == 4) || ($sensor == 5))
    {
        $metadata{"sceneStartTime"} = $metaInfo_ref->{start_time};
        $metadata{"sceneStopTime"} = $metaInfo_ref->{stop_time};
        $metadata{"imageQuality"} = $metaInfo_ref->{acquisition_quality};
        $metadata{"sceneSource"} = $metaInfo_ref->{scene_source};
 
        # format satellite number
        $metadata{"satelliteNumber"} = Satellite_Number($sensor);
        
        # format Condition value
        $metadata{"condition"} = $metaInfo_ref->{sensor_anomalies};

        # format Order Status value
        $metadata{"orderStatus"}
                = Order_Status($metaInfo_ref->{prod_media_type});

        # format Descending or Ascending value
        $metadata{"flightPath"} = Desc_Ascend("D");

        # set the browse resolution
        if ($metaInfo_ref->{scene_source}
            && $metaInfo_ref->{scene_source} eq "LAM")
        {
            $metadata{"resolution"} = "240 meters";
        }
        else
        {
            $metadata{"resolution"} = "480 meters";
        }
    }
    elsif ($sensor == 7)
    {
        $metadata{"sceneStartTime"} = $metaInfo_ref->{start_time};
        $metadata{"sceneStopTime"} = $metaInfo_ref->{stop_time};
        $metadata{"imageQuality1"} = $metaInfo_ref->{image_quality_vcid_1};
        $metadata{"imageQuality2"} = $metaInfo_ref->{image_quality_vcid_1};
        $metadata{"gainChangeBand1"} = $metaInfo_ref->{gain_change_band_1};
        $metadata{"gainChangeBand2"} = $metaInfo_ref->{gain_change_band_2};
        $metadata{"gainChangeBand3"} = $metaInfo_ref->{gain_change_band_3};
        $metadata{"gainChangeBand4"} = $metaInfo_ref->{gain_change_band_4};
        $metadata{"gainChangeBand5"} = $metaInfo_ref->{gain_change_band_5};
        $metadata{"gainChangeBand6L"}
                = $metaInfo_ref->{gain_change_band_6_vcid_1};
        $metadata{"gainChangeBand6H"}
                = $metaInfo_ref->{gain_change_band_6_vcid_2};
        $metadata{"gainChangeBand7"} = $metaInfo_ref->{gain_change_band_7};
        $metadata{"gainChangeBand8"} = $metaInfo_ref->{gain_change_band_8};

        # format Gain Bands 1-8 values
        $metadata{"gainBand1"} = High_Low($metaInfo_ref->{gain_band_1});
        $metadata{"gainBand2"} = High_Low($metaInfo_ref->{gain_band_2});
        $metadata{"gainBand3"} = High_Low($metaInfo_ref->{gain_band_3});
        $metadata{"gainBand4"} = High_Low($metaInfo_ref->{gain_band_4});
        $metadata{"gainBand5"} = High_Low($metaInfo_ref->{gain_band_5});
        $metadata{"gainBand6L"} = High_Low($metaInfo_ref->{gain_band_6_vcid_1});
        $metadata{"gainBand6H"} = High_Low($metaInfo_ref->{gain_band_6_vcid_2});
        $metadata{"gainBand7"} = High_Low($metaInfo_ref->{gain_band_7});
        $metadata{"gainBand8"} = High_Low($metaInfo_ref->{gain_band_8});

        # format Descending or Ascending value
        $metadata{"flightPath"} = Desc_Ascend("D");

        # set the browse resolution
        if ($metaInfo_ref->{lines} < 900 )
        {
            $metadata{"resolution"} = "240 meters";
        }
        else
        {
            $metadata{"resolution"} = "180 meters";
        }
    }
    elsif ($sensor == 8)
    {
        $metadata{"sceneStartTime"} = $metaInfo_ref->{start_time};
        $metadata{"sceneStopTime"} = $metaInfo_ref->{stop_time};
        $metadata{"imageQuality"} = $metaInfo_ref->{image_quality};

        # format Descending or Ascending value
        $metadata{"flightPath"} = Desc_Ascend("D");

        # set the browse resolution
        $metadata{"resolution"} = "240 meters";
    }

    # Landsat 4/5 TM Metadata layout
    my @order;
    if (($sensor == 4) || ($sensor == 5))
    {
        #This is the order we want the items to appear in the L5(TM) table
        @order = ("sceneId",
                  "acquisitionDate",
                  "path",
                  "row",
                  "sensor",
                  "resolution",
                  "upperLeftCornerLatitude",
                  "upperLeftCornerLongitude",
                  "upperRightCornerLatitude",
                  "upperRightCornerLongitude",
                  "lowerLeftCornerLatitude",
                  "lowerLeftCornerLongitude",
                  "lowerRightCornerLatitude",
                  "lowerRightCornerLongitude",
                  "sceneCenterLatitude",
                  "sceneCenterLongitude",
                  "cloudCover",
                  "satelliteNumber",
                  "browseAvailable",
                  "browseRedBandNumber",
                  "browseRedBandType",
                  "browseGreenBandNumber",
                  "browseGreenBandType",
                  "browseBlueBandNumber",
                  "browseBlueBandType",
                  "dayOrNight",
                  "sunElevation",
                  "sunAzimuth",
                  "receivingStation", 
                  "sceneStartTime",
                  "sceneStopTime", 
                  "imageQuality",
                  "bandQuality",
                  "condition", 
                  "sceneSource",
                  "orderStatus"
                 );
     }
     # Landsat 7 ETM Metadata layout
     elsif ($sensor == 7)
     {
        # This is the order we want the items to appear in the L7(ETM) table
        @order = ("sceneId",
                  "acquisitionDate",
                  "path",
                  "row",
                  "sensor",
                  "resolution",
                  "upperLeftCornerLatitude",
                  "upperLeftCornerLongitude",
                  "upperRightCornerLatitude",
                  "upperRightCornerLongitude",
                  "lowerLeftCornerLatitude",
                  "lowerLeftCornerLongitude",
                  "lowerRightCornerLatitude",
                  "lowerRightCornerLongitude",
                  "sceneCenterLatitude",
                  "sceneCenterLongitude",
                  "cloudCover",
                  "browseAvailable",
                  "browseRedBandNumber",
                  "browseRedBandType",
                  "browseGreenBandNumber",
                  "browseGreenBandType",
                  "browseBlueBandNumber",
                  "browseBlueBandType",
                  "dayOrNight",
                  "flightPath",
                  "sunElevation",
                  "sunAzimuth",
                  "receivingStation", 
                  "sceneStartTime",
                  "sceneStopTime",
                  "imageQuality1",
                  "imageQuality2",
                  "gainBand1",
                  "gainBand2", 
                  "gainBand3",
                  "gainBand4", 
                  "gainBand5",
                  "gainBand6H",
                  "gainBand6L",
                  "gainBand7", 
                  "gainBand8", 
                  "gainChangeBand1",
                  "gainChangeBand2",
                  "gainChangeBand3",
                  "gainChangeBand4",
                  "gainChangeBand5",
                  "gainChangeBand6H",
                  "gainChangeBand6L",
                  "gainChangeBand7", 
                  "gainChangeBand8"
                 );
    }
    elsif ($sensor == 8)
    {
        # This is the order we want the items to appear in the L8(OLI) table
        @order = ("sceneId",
                  "acquisitionDate",
                  "path",
                  "row",
                  "sensor",
                  "resolution",
                  "upperLeftCornerLatitude",
                  "upperLeftCornerLongitude",
                  "upperRightCornerLatitude",
                  "upperRightCornerLongitude",
                  "lowerLeftCornerLatitude",
                  "lowerLeftCornerLongitude",
                  "lowerRightCornerLatitude",
                  "lowerRightCornerLongitude",
                  "sceneCenterLatitude",
                  "sceneCenterLongitude",
                  "cloudCover",
                  "browseAvailable",
                  "browseRedBandNumber",
                  "browseRedBandType",
                  "browseGreenBandNumber",
                  "browseGreenBandType",
                  "browseBlueBandNumber",
                  "browseBlueBandType",
                  "dayOrNight",
                  "flightPath",
                  "sunElevation",
                  "sunAzimuth",
                  "receivingStation", 
                  "sceneStartTime",
                  "sceneStopTime",
                  "imageQuality"
                 );
    }

    print "    <!-- primary browse image associated metadata -->\n\n";
    print "    <metaData>\n";
    foreach my $metaItem (@order)
    {
        $metadata{$metaItem} = "" unless $metadata{$metaItem};
        print "        <$metaItem>$metadata{$metaItem}</$metaItem>\n";
    }
    print "    </metaData>\n";
}

#!/usr/bin/perl -wT
#############################################################################
# getOverviewMap.pl
#    perl cgi module to create an overview map with the passed in lat/long
#    indicated with an icon.  The width and height of the map can be specified
#    with optional width and height parameters.
#
#############################################################################

use strict;

# add the directory for the GD library
use lib "perllib";

# use the GD and CGI modules
use GD;
use CGI qw(:standard escapeHTML);

# get the lat/long passed in
my $lat = param('lat');
my $lon = param('lon');

# get the optional width and height parameters
my $req_width = param('width');
my $req_height = param('height');

# do some validation on the parameters
$lat = 90 if ($lat > 90);
$lat = -90 if ($lat < -90);
while ($lon < -180)
{
    $lon += 360;
}
while ($lon > 180)
{
    $lon -= 360;
}

# set the map image
my $mapImage = "../graphics/World5Minutewithborder780.jpg";

# read the input image
open (FILE,"<$mapImage") or die "Error Opening map image file";
my $image = GD::Image->newFromJpeg(\*FILE);
close FILE;

# get the image size
my ($width,$height) = $image->getBounds();

# default the requested width and height if they are not defined
$req_width = $width if (not defined $req_width);
$req_height = $height if (not defined $req_height);

# limit the width and height to the size of the input image
$req_width = $width if ($req_width > $width);
$req_height = $height if ($req_height > $height);

# make sure the width and height are at least a minimum of 100
$req_width = 100 if ($req_width < 100);
$req_height = 100 if ($req_height < 100);

# create the output image (need to do this since color allocations fail on
# the already read image)
my $outputImage = GD::Image->new($req_width, $req_height);

# allocate the colors for the location indication
my $red = $outputImage->colorAllocate(255, 0, 0);
my $black = $outputImage->colorAllocate(0, 0, 0);

# calculate the location for the icon on the map
my $x = int($width * ($lon + 180) / 360);
my $y = int(- $height * ($lat - 90) / 180);

# calculate the starting point
my $ulx = $x - int($req_width/2);
my $uly = $y - int($req_height/2);

# make sure the upper-left corner remains in the image
$ulx = 0 if ($ulx < 0);
$uly = 0 if ($uly < 0);

# make sure the lower-right corner remains in the image
$ulx = $width - $req_width if (($ulx + $req_width) > $width);
$uly = $height - $req_height if (($uly + $req_height) > $height);

# adjust the x/y location for the displayed upper left corner
$x -= $ulx;
$y -= $uly;

# copy the window of the source image to the output image
$outputImage->copy($image, 0, 0, $ulx, $uly, $req_width, $req_height);

# create a diamond for the icon
my $poly = new GD::Polygon;
$poly->addPt(5, 0);
$poly->addPt(10, 5);
$poly->addPt(5, 10);
$poly->addPt(0, 5);
$poly->offset($x-5, $y-5);

# create a shadow diamod for below the real diamond
my $shadowPoly = new GD::Polygon;
$shadowPoly->addPt(6, 0);
$shadowPoly->addPt(12, 6);
$shadowPoly->addPt(6, 12);
$shadowPoly->addPt(0, 6);
$shadowPoly->offset($x-6, $y-6);

# draw the diamond indicator with the shadow below it
$outputImage->filledPolygon($shadowPoly, $black);
$outputImage->filledPolygon($poly, $red);

# write the final image
print "Content-type: image/jpeg\n\n";
print ($outputImage->jpeg(75));

exit 0;


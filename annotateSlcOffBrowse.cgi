#!/usr/bin/perl -wT
#############################################################################
# annotateSlcOffBrowse.pl
#    perl cgi module to create the annotated SLC-off browse image
#
#############################################################################

use strict;

# add the directory for the GD library
use lib "perllib";

# use the GD and CGI modules
use GD;
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $annotateFlag = param('annotate');
my $image = param('image');

# get the command line parameters
my $inFile = $image;

# set up some constants for the annotation
my $borderTop = 30;
my $borderBottom = 30;
my $sideBorder = 0.03/7;
my $leftLinePercent = 0.435 + $sideBorder;
my $rightLinePercent = 1 - .435 - $sideBorder;

# original image info
my $origHeight;
my $origWidth;

# colors for the output image annotation
my $black;
my $yellow;
my $white;

# read the input image
# Note: original code had the following line, but it doesn't work with 
#       the version of GD.pm we have
#$this->{'origImage'} = GD::Image->newFromJpeg($this->{'inFile'},1)
open (FILE,"<$inFile") or die "Error Opening $inFile: $!";
my $origImage = GD::Image->newFromJpeg(\*FILE);
close FILE;

# get the image size
($origWidth,$origHeight) = $origImage->getBounds();

# copy the image to a new image to annotate
my $outputImage = createAnnotationImage($origImage);

# add the spacing annotation to the display if requested
if ($annotateFlag == 1)
{
    addSpacingAnnotation($outputImage);
}

# write the output image
print "Content-type: image/jpeg\n\n";
print ($outputImage->jpeg(75));

exit 0;

# create a version of the image to annotate
sub createAnnotationImage
{
    my $origImage = shift;
    my $imageHeight = $origHeight + $borderTop + $borderBottom;

    # Note: original code had a third parameter "1" passed in, but the version
    #       of GD.pm we have doesn't accept it
    my $image = GD::Image->new($origWidth,$imageHeight);
    
    # allocate the colors needed for image annotation (can't wait until after
    # the original image is copied since it fails to allocate additional
    # colors after that)
    $black = $image->colorAllocate(0,0,0);
    $white = $image->colorAllocate(255,255,255);
    $yellow = $image->colorAllocate(245,245,0);
    
    #fill the new image with black
    $image->fill(0,0,$black);

    #copy the original image on top of the new image
    $image->copy($origImage, 0, $borderTop, 0, 0, $origWidth, $origHeight);
    
    return $image;
}

# creates the slc-off "unaffected area" lines on the image
sub addUnaffectedLines
{
    $image = shift;
    my ($imageWidth,$imageHeight) = $image->getBounds();

    $image->filledRectangle( $leftLinePercent * $imageWidth - 1,
            $borderTop, $leftLinePercent * $imageWidth,
            $imageHeight - $borderBottom, $yellow);
            
    $image->filledRectangle($rightLinePercent * $imageWidth, $borderTop,
            $rightLinePercent * $imageWidth + 1, $imageHeight - $borderBottom,
            $yellow);
}

# draw one set of annotation lines
sub drawAnnotation
{
    my $image = shift;
    my $font = shift;
    my $difference = shift;
    my $number = shift;
    my $lineType = shift;

    my ($imageWidth,$imageHeight) = $image->getBounds();
    my $len = length($number);

    $image->string($font, $leftLinePercent * $imageWidth - ($font->width * $len)
                   - $difference * $imageWidth,
                   $borderTop - $font->height - $font->height / 2, $number, 
                   $white);
    $image->string($font, ($rightLinePercent + $difference) * $imageWidth,
                   $borderTop - $font->height - $font->height / 2, $number, 
                   $white);                              
    $image->string($font, $leftLinePercent * $imageWidth - ($font->width * $len)
                  - $difference * $imageWidth,
                  $imageHeight - $borderBottom / 2 - $font->height / 2, 
                  $number, $white);
    $image->string($font, ($rightLinePercent + $difference) * $imageWidth,
                   $imageHeight - $borderBottom / 2 - $font->height / 2, 
                   $number, $white);
    if ($lineType eq 'solid')
    {
        # add the "unaffected area" lines
        addUnaffectedLines($image);
    }
    else
    {
        createDottedLine($image, $leftLinePercent * $imageWidth 
                - ($len * $font->width) / 2 - $difference * $imageWidth);
        createDottedLine($image, $rightLinePercent * $imageWidth
                + ($len * $font->width) / 2 + $difference * $imageWidth);
    }

}

# adds the numbers to the top and bottom of the images
sub addSpacingAnnotation
{
    my $image = shift;
    my $font = GD::Font->Giant;
    my ($imageWidth,$imageHeight) = $image->getBounds();
    
    # 2 pixel
    my $difference = - $sideBorder;
    drawAnnotation($image,$font,$difference,'2','solid');

    # 4 pixel
    $difference = $leftLinePercent - .364 - $sideBorder * 2;
    drawAnnotation($image,$font,$difference,'4','dotted');

    # 6 pixel
    $difference = $leftLinePercent - .293 - $sideBorder * 3;
    drawAnnotation($image,$font,$difference,'6','dotted');

    # 8 pixel
    $difference = $leftLinePercent - .221 - $sideBorder * 4;
    drawAnnotation($image,$font,$difference,'8','dotted');

    # 10 pixel
    $difference = $leftLinePercent - .150 - $sideBorder * 5;
    drawAnnotation($image,$font,$difference,'10','dotted');
                                            
    # 12 pixel
    $difference = $leftLinePercent - .079 - $sideBorder * 6;
    drawAnnotation($image,$font,$difference,'12','dotted');

    # 14 pixel
    $difference = $leftLinePercent - .008 - $sideBorder * 7;
    drawAnnotation($image,$font,$difference,'14','dotted');
}

# creates the dotted lines for the 4 thru 14 spaces
sub createDottedLine
{
    my $image = shift;
    my $x = shift;
    my ($imageWidth,$imageHeight) = $image->getBounds();
    
    # we'll start with a 15 pixel bar size and 15 pixel gap between bars
    my $barSize = 5;
    
    for(my $y = $borderTop; $y < $imageHeight - $borderBottom; 
        $y += $barSize * 3)
    {
        $image->filledRectangle($x-.5,$y,$x+.5,$y + $barSize,$yellow);
    }
}       

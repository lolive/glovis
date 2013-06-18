#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# Landsat scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $sceneid = param('scene_id');
$_ = $sceneid;

# make sure it is really a Landsat scene id
if ($sceneid !~ /^L[OCEMT]\d{14}\w{3}\d{2}$/)
{
    handleerror("Scene ID $sceneid is not legal"); # exits
}
# Note: O = OLI (L8), C = Combined OLI/TIRS (L8), E = ETM (L7), M = MSS (L4-5)
# T = TM if sensor is 4 or 5, could be TIRS if 8 but GloVis doesn't use thermal

# extract the components needed from the scene id to build the path to the 
# actual image
my $mode = substr($sceneid,1,1);
my $sensor = substr($sceneid,2,1);
my $path = substr($sceneid,3,3);
my $row = substr($sceneid,6,3);
my $year = substr($sceneid,9,4);

# make a version of the sensor for the path to the image since L4 and 5 are
# both under the l5 directory
my $sensorpath = $sensor;
$sensorpath .= "oli" if ($sensor == 8);
$sensorpath = "5" if ($sensor == 4);

# Because the TM and MSS sensor are aboard Landsat 4 and 5 satellites we have
# to use the sensor number and the instrument mode to find the correct
# directory path for the right sensor type: (4/5 TM = /l5),(4/5 MSS = /l4_5mss),
# & (ETM = /l7)
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
my $image = "";
my $is_slcoff = 0;
my $metafile = "";
my $line;

if ($sensor == 8)
{
    # for Landsat 8 OLI, read the browse name from the metadata file
    $metafile = $dirname . "/" . $sceneid . ".meta";

    $line = readMetaFile($metafile);     
    
    # build the path to the image
    $image = $dirname . "/" . $line;
}
elsif ($sensor == 7)
{
    # for Landsat 7, build the file name for the .meta file and read the
    # original scene file name from the file.  For some reason the LAM used
    # very dumb names for the early L7 browse scenes, so this is the generic
    # method to pick the proper name for the file
    $metafile = $dirname . "/" . $sceneid . ".meta";

    # if the metadata file doesn't exist, look in the SLC-off inventory instead
    if (! -e $metafile)
    {
        if ($year >= 2003)
        {
            my $slc_off_dirname = "l7slc_off/p" . $path . "/r" . $row . 
                "/y" . $year;
            my $alternate_metafile = $slc_off_dirname . "/" . $sceneid 
                . ".meta";

            if (-e $alternate_metafile)
            {
                $metafile = $alternate_metafile;
                $dirname = $slc_off_dirname;
                $is_slcoff = 1;
            }
        }
    }
    
    $line = readMetaFile($metafile);

    # build the path to the image
    $image = $dirname . "/" . $line;

    # attach ".jpg" if it isn't already there since some have it and others
    # don't
    $_ = $line;
    $image = $image . ".jpg" if (not /.jpg$/);
}
elsif ((($sensor == 4) || ($sensor == 5)) && ($mode eq "T"))
{
    # for Landsat 4/5 TM, read the browse name from the metadata file
    $metafile = $dirname . "/" . $sceneid . ".meta";

    $line = readMetaFile($metafile);     
    
    # build the path to the image
    $image = $dirname . "/" . $line;
}
elsif (($sensor <= 5) && ($mode eq "M"))
{
    # for Landsat MSS, read the browse name from the metadata file
    $metafile = $dirname . "/" . $sceneid . ".meta";

    $line = readMetaFile($metafile);   
    # build the path to the image
    $image = $dirname . "/" . $line;
}

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $initscript=<<END;
// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isIE4=(document.all && !document.getElementById)?true:false;
    isIE5=(document.all && document.getElementById)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    if (document.images)
    {
        winHeight = document.images[0].height + 170
        winWidth = document.images[0].width + 50
        // adjust the size if Net 4
        if (isNS4)
        {
            winHeight = winHeight - 150
            winWidth = winWidth - 30
        }
        // make sure we do not exceed screen size
        if (winHeight > screen.availHeight)
            winHeight = screen.availHeight
        if (winWidth > screen.availWidth)
            winWidth = screen.availWidth

        resizeTo(winWidth, winHeight)
    }
}
END

my $slcoffscript=<<END;
// Pre-fetch the two images
browseimages = new Array(2);

for (i=0; i<2; i++) {
	browseimages[i] = new Image();
	browseimages[i].src = "annotateSlcOffBrowse.cgi?annotate=" + i 
        + "&image=$image";
}

function toggle() {
	if (document.grid_toggle_form.grid_toggle.checked)
		document.images['browse'].src = browseimages[1].src;
	else
		document.images['browse'].src = browseimages[0].src;
}

END

# build the javascript code needed for this sensor
my $jscript = "";
if ($is_slcoff == 1)
{
    $jscript .= $slcoffscript;
}
$jscript .= $initscript;

# build the web page
print header();
print start_html(-title=>"USGS Global Visualization Viewer - Scene $sceneid",
                 -script=>$jscript, -onLoad=>'init()');
if ($is_slcoff == 1)
{
print<<END
<font face = "Verdana, Arial, Helvetica" size=-2 >This Landsat 7 browse image 
has been enhanced to illustrate the distribution of image effects due to the 
SLC-off mode of acquisition. The numerical scale across the top and bottom of 
the image represents the width (in 30-meter image pixels) of duplicated lines,
as they exist within the Level 0Rp (raw uncorrected, reformatted) product. 
A standard 2-pixel interpolation is applied during Level 1 processing of all 
SLC-off Landsat 7 imagery. The area between the two solid lines will 
represent the approximate portion of the scene that contains no missing data 
in the final Level 1 product, after the 2-pixel interpolation is
applied. The dashed lines indicate the width (in 30-meter image pixels) of 
expected data gaps in the final Level 1 product.
For more information on SLC-off data, please visit:
<a href="http://landsat.usgs.gov/Landsat_7_ETM_SLC_off_data.php"
target="_blank">Landsat 7 SLC-off Data Products</a>.</font><br><br>
<form name="grid_toggle_form">
<input name="grid_toggle" type="checkbox" checked="1" 
onclick="javascript:toggle()">Show grid
</form>
<img SRC="annotateSlcOffBrowse.cgi?annotate=1&image=$image" name="browse"
 alt="This is the browse image for selected scene">
<noscript>
    <p>
    You have Javascript turned off in your browser. No annotations are shown.
    <img src="$image" name="browse"
        alt="This is the browse image for the selected scene">
</noscript>
END
}
else
{
    print img {src=>$image};
}
print end_html();
exit;

sub handleerror
{
    my @messages = @_; # for debugging - do not print details in prod
    print header();
    print start_html("USGS Global Visualization Viewer - Browse Error");
    print p("Error generating browse image page");
    #foreach (@messages)
    #{
    #    print p("$_");
    #}
    print end_html();
    exit;
}

# routine to read the local browse file name from the metadata file
sub readMetaFile
{
    my($metafile) = @_;
    
    open (META, "<".$metafile) or handleerror("Can't open $metafile: $!");

    my $line = "";
    while ($line = <META>)
    {
        my($name,$value) = split (/\s*=\s*/,$line);

        if ($name eq "LocalBrowseName")
        {
            $line = $value;
            last;
        }
    }
    close(META);

    chomp $line;
    return $line;
}


#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

my %mrlcDir = ("MRLC2K_ARCH"=>"mrlc_2001_tc",  # Terrain Corrected (Radiance)
               "MRLC2K_SITC"=>"mrlc_2001_ra"   # Reflectance Adjusted
              );

# get the scene id and dataset passed in
my $tainted_sceneid = param('scene_id');
my $tainted_dataset = param('dataset');
my $sensorpath;

# make sure it is really a scene id
if ($tainted_sceneid !~ m/^[A-Z0-9]{21}$/)
{
    handleerror("Bad sceneid $tainted_sceneid");
}

# Because of the two different mrlc database tables for mrlc 2001 terrain
# corrected and 2001 reflectance adjusted there are two different directories
# that the data is stored. Mrlc 2001 Terrain Corrected (mrlc_2001_tc, and
# Mrlc 2001 Reflectance Adjusted (mrlc_2001_ra).
if (($tainted_dataset =~ /\w{11}/) and $mrlcDir{$tainted_dataset})
{
    $sensorpath = $mrlcDir{$tainted_dataset};
}
else
{
    # not a recognized dataset
    handleerror("Unrecognized dataset $tainted_dataset");
}

# extract the components needed from the scene id to build the path to the 
# actual image
my $path = 0;
my $row = 0;
my $year = 0;
my $sceneid = '';
if ($tainted_sceneid =~ /^(L[ET]\d{14}[A-Z]{3}\d{2})$/) # just like a Landsat ID
{
    $sceneid = $1;
    $path = substr($sceneid,3,3);
    $row  = substr($sceneid,6,3);
    $year = substr($sceneid,9,4);
}
elsif ($tainted_sceneid =~ /^([A-Z]{3}\w\d{17})$/) # or an MRLC ID
{
    $sceneid = $1;
    $path = substr($sceneid,5,3);
    $row  = substr($sceneid,8,3);
    $year = substr($sceneid,15,4);
}
else
{
    handleerror("Can't determine path, row, year from $tainted_sceneid");
}

# build the directory name for this sensor/path/row/year
my $dirname = $sensorpath . "/p" . $path . "/r" . $row . "/y" . $year;
my $image = "";
my $line;

# Build the file name for the .meta file and read the original scene file
# name in that file.  For some reason the LAM used very dumb names for 
# the early L7 browse scenes, so this is the generic method to pick the
# proper name for the file
my $metafile = $dirname . "/" . $sceneid . ".meta";

$line = readMetaFile($metafile);

# build the path to the image
$image = $dirname . "/" . $line;

# attach ".jpg" if it isn't already there since some have it and others
# don't
$_ = $line;
$image = $image . ".jpg" if (not /.jpg$/);

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;
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

# build the web page
print header();
print start_html(-title=>"USGS Global Visualization Viewer - Scene $sceneid",
                 -script=>$JSCRIPT, -onLoad=>'init()');
print img {src=>$image};
print end_html();
exit;

sub handleerror
{
    my @messages = @_; # for debugging - do not print details in prod

    print header();
    print start_html("USGS Global Visualization Viewer - Error");
    print p("Error generating browse image page");
    #foreach (@messages)
    #{
    #    print p($_);
    #}
    print end_html();
    exit;
}

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

    return $line;
}


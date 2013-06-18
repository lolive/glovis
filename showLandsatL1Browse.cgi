#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

my %l1tDir = ("GLS2010"        => "gls/gls2010",
              "GLS2005"        => "gls/gls2005",
              "GLS2000"        => "gls/gls2000",
              "GLS1990"        => "gls/gls1990",
              "GLS1975_4_5MSS" => "gls/gls1975_mss4_5",
              "GLS1975_1_3MSS" => "gls/gls1975_mss1_3",
              "LIMA"           => "lima",
              "SYSTEMATIC_L1G" => "lsat_sys"
             );

# get the scene id and dataset passed in
my $tainted_sceneid = param('scene_id');
my $tainted_dataset = param('dataset');

# make sure it is really a scene id for one of the L1 datasets and extract
# some needed info from the scene id, determine sensorpath based on dataset
my $sensorpath;
my $path;
my $row;
my $year;

if (($tainted_dataset =~ /\w/) and $l1tDir{$tainted_dataset})
{
    $sensorpath = $l1tDir{$tainted_dataset};
}
else
{
    # not a recognized dataset
    handleerror("Unrecognized dataset $tainted_dataset");
}

if ($tainted_sceneid =~ /^P\d{3}R\d{3}_\d\w\d{8}$/)
{
    $path = "p" . substr($tainted_sceneid,1,3);
    $row = "r" . substr($tainted_sceneid,5,3);
    $year = substr($tainted_sceneid,11,4);
}
elsif ($tainted_sceneid =~ /^L[ET]\d{14}\w{3}\d{2}$/)
{
    $path = "p" . substr($tainted_sceneid,3,3);
    $row = "r" . substr($tainted_sceneid,6,3);
    $year = substr($tainted_sceneid,9,4);
}
elsif ($tainted_sceneid =~ /^L71\d{6}_\d{11}$/)
{
    $path = "p" . substr($tainted_sceneid,3,3);
    $row = "r" . substr($tainted_sceneid,6,3);
    $year = substr($tainted_sceneid,13,4);
}
elsif ($tainted_sceneid =~ /^L[ET]\d{16}$/)
{
    my $tainted_path = param('path');
    my $tainted_row = param('row');
    if ($tainted_path =~ /^([EW]\d{1,3})$/)
    {
        $path = $1;
    }
    if ($tainted_row =~ /^([NS]\d{1,3})$/)
    {
        $row = $1;
    }
    $year = substr($tainted_sceneid,11,2);
    if (int($year) > 98)
    {
        $year = "19" . $year;
    }
    else
    {
        $year = "20" . $year;
    }
}
if (not defined $path)
{
    # not a recognized scene id
    handleerror("No path, could not parse sceneid $tainted_sceneid");
}

# build the directory name for this sensor/path/row/year
my $dirname = $sensorpath . "/" . $path . "/" . $row . "/y" . $year;
my $sceneid = $tainted_sceneid;

# read the browse name from the metadata file
my $metafile = $dirname . "/" . $sceneid . ".meta";

my $line = readMetaFile($metafile);   

# build the path to the image
my $image = $dirname . "/" . $line;

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

# build the web page
print header();
print start_html(-title=>"USGS Global Visualization Viewer - $sceneid Browse",
                 -script=>$initscript, -onLoad=>'init()');
print img {src=>$image};
print end_html();
exit;

sub handleerror
{
    my @messages = @_; # for debugging - don't print details in prod
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


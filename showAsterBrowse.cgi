#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# scene id, path, and row passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);
use File::Find;

# set up top-level inventory subdirectories for each dataset
my %asterdir = ("ASTER_VNIR" => "aster/vnir",
                "ASTER_TIR"  => "aster/tir",
                "ASTER_VNIR_DATAPOOL" => "aster_datapool/vnir",
                "ASTER_TIR_DATAPOOL"  => "aster_datapool/tir"
               );

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $tainteddataset = param('dataset');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $sceneid;
my $subdir;
my $path;
my $row;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
if ($taintedsceneid =~ /^(\d{3}):(\d{5,10})$/)
{
    $sceneid = $2;
}
elsif ($taintedsceneid =~ /^(\d{5,10})$/)
{
    $sceneid = $1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal"); # exits
}

# find the top-level subdirectory from the dataset
if ($asterdir{$tainteddataset})
{
    $subdir = $asterdir{$tainteddataset};
}
else
{
    handleerror("Dataset $tainteddataset is not legal"); # exits
}

# untaint the path and row
if ($taintedpath =~/^(\d{1,3})$/)
{
    $path = $1;
    $path = sprintf("%03d", $path);
}
else
{
    handleerror("Path $taintedpath is not legal"); # exits
}
if ($taintedrow =~/^(\d{1,3})$/)
{
    $row = $1;
    $row = sprintf("%03d", $row);
}
else
{
    handleerror("Row $taintedrow is not legal"); # exits
}

# build the directory name for this sensor/path/row/year, using the subdir
my $dirname = $subdir . "/p" . $path . "/r" . $row;

# find the scene id
my $image = "";
my $name = $sceneid . ".jpg";
File::Find::find({wanted => sub {
        if (/$name/)
        {
            $image = $File::Find::name;
        }
    }, untaint => 1},$dirname);


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
    my @msgs = (); # for debugging - do not print details in prod
    print header();
    print start_html("USGS Global Visualization Viewer - Browse Error");
    print p("Error generating browse image page");
    #foreach (@msgs)
    #{
    #    print p($_);
    #}
    print end_html();
    exit;
}

#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# scene id, gridcol, and gridrow passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);
use File::Find;

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $sceneid = "";
my $gridcol = "";
my $gridrow = "";

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
if ($taintedsceneid =~ /^(\w{7}\d{9})$/)
{
    $sceneid = $1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal");
}

# untaint the column and row
if ($taintedpath =~/^(\d{4})$/)
{
    $gridcol = $1;
}
else
{
    handleerror("Path $taintedpath is not legal");
}
if ($taintedrow =~/^(\d{4})$/)
{
    $gridrow = $1;
}
else
{
    handleerror("Row $taintedrow is not legal");
}

# build the directory name for this sensor/gridcol/gridrow
my $dirname = "nhap/";
if (substr($sceneid, 1, 1) eq "B")
{
    $dirname .= "bw/";
}
else
{
    $dirname .= "cir/";
}
$dirname .= $gridcol . "/" . $gridrow;

# find the scene id in the correct year directory
my $image = "";
my $name = $sceneid . "_unclipped.jpg";
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
        winHeight = document.images[0].height + 180
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
    my @messages = @_; # mainly used for debugging - don't print details in prod
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

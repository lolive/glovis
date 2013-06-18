#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# NALC scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $sceneid;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# untaint the scene id
if ($taintedsceneid =~ /^(LPNALC\d{6}\w)$/)
{
    $sceneid = $1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal");
}

# extract the path and row from the scene id
my $path = substr($sceneid, 6, 3);
my $row = substr($sceneid, 9, 3);
if (($path < 1) || ($path > 233) || ($row < 1) || ($row > 124))
{
    handleerror("Scene ID $taintedsceneid is not legal");
}

# build the browse name for this dataset/path/row/sceneid
my $dirname = "nalc/p$path/r$row";

# set the image name
my $image = "$dirname/$sceneid.jpg";

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

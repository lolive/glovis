#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# Tri-decadal ETM+ mosaic scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id, path, and row passed in
my $taintedsceneid = param('scene_id');
my $sceneid;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# location for a TM mosaic
my $search_for_TM = 0;

# untaint the scene id
if ($taintedsceneid =~ /^(\w{3}-\d{2}-\d{2}_\w{2}_\d{4})$/)
{
    $sceneid = $1;
}
elsif ($taintedsceneid =~ /^(\w{3}-\d{2}-\d{2}_\w{3})$/)
{
    $sceneid = $1;
}
elsif ($taintedsceneid =~ /^(\w{3}-\d{2}-\d{2}_\d{2}_\w{3})$/)
{
    $sceneid = $1;
    $search_for_TM = 1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal");
}

my $column;
my $row;
my $image;
if ($sceneid =~ /^MT/)
{
    # TM mosaic.  find the column and row from the scene id
    $column = substr($sceneid, 4, 2);
    $row = substr($sceneid, 2, 1) . substr($sceneid, 7, 2);

    # set the image name
    $image = "ortho/tm_mosaic/$column/$row/$sceneid.jpg";

    # if we need to search for the TM mosaic, if it doesn't exist at the 
    # current location, check the alternate row
    if (($search_for_TM) && ! -e $image)
    {
        $row = substr($sceneid, 2, 1) . substr($sceneid, 10, 2);
        $image = "ortho/tm_mosaic/$column/$row/$sceneid.jpg";
    }
}
else
{
    # ETM mosaic.  find the column and row from the scene id
    $column = substr($sceneid, 4, 2) . substr($sceneid, 11, 1);
    $row = substr($sceneid, 2, 1) . substr($sceneid, 7, 2)
         . substr($sceneid, 10, 1);

    # set the image name
    $image = "ortho/etm_mosaic/$column/$row/$sceneid.jpg";
}

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
    print start_html("USGS Global Visualization Viewer - Scene $sceneid");
    print p("Error generating browse image page");

    # Un-comment this for debugging
    #foreach(@messages)
    #{
    #    print p($_);
    #}
    print end_html();
    exit;
}

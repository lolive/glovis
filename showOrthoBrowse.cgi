#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# scene id, path, and row passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $sceneid;
my $path;
my $row;
my $sat_num;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
# For mss scene ids, we have to retrieve the satellite number to determine
# mss1-3 vs. mss4-5.
my $platform;
if ($taintedsceneid =~ /^(ET[\d\w]{18})$/)
{
    $sceneid = $1;
    $platform = "tm";
}
elsif ($taintedsceneid =~ /^(EL[\d\w]{19})$/)
{
    $sceneid = $1;
    $platform = "etm";
}
elsif ($taintedsceneid =~ /^(EP[\d\w]{19})$/)
{
    $sceneid = $1;
    $platform = "etmpan";
}
elsif ($taintedsceneid =~ /^(EM[\d\w]{18})$/)
{
    $sceneid = $1;
    $sat_num = substr($sceneid,10,1);
    if ($sat_num < 4)
    {
        $platform = "mss1_3";
    }
    else
    {
        $platform = "mss4_5";
    }
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal");
}

# untaint the path and row
if ($taintedpath =~/^(\d{1,3})$/)
{
    $path = $1;
    $path = sprintf("%03d", $path);
}
else
{
    handleerror("Path $taintedpath is not legal");
}
if ($taintedrow =~/^(\d{1,3})$/)
{
    $row = $1;
    $row = sprintf("%03d", $row);
}
else
{
    handleerror("Row $taintedrow is not legal");
}

# build the browse name for this dataset/path/row/year/sceneid
my $dirname;
$dirname = "ortho/tm" if ($platform eq "tm");
$dirname = "ortho/etm" if ($platform eq "etm");
$dirname = "ortho/pansharp_etm" if ($platform eq "etmpan");
$dirname = "ortho/mss1_3" if ($platform eq "mss1_3");
$dirname = "ortho/mss4_5" if ($platform eq "mss4_5");
$dirname .= "/p" . $path . "/r" . $row;
my $year;
my $image;
if (($platform eq "tm") || ($platform eq "mss1_3") || ($platform eq "mss4_5"))
{
   $year = substr($sceneid,12,4);
}
else
{
   $year = substr($sceneid,13,4);
}

# set the image name
$image = "$dirname/y$year/$sceneid.jpg";

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

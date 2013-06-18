#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# MODIS scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $sceneid;
my $secondaryid = param('secondary_id');
my $taintedbrowsenumber = param('browse_number');
my $browsenumber = 0;
my $browsenumext = ''; # extension to file name if it has a browse number
my $dataset = lc(param('dataset'));

# make sure it is really a MODIS secondary id
if ($secondaryid
    !~ /^\w{6,8}\.A\d{7,7}\.h\d{2,2}v\d{2,2}\.\d{3,3}\.\d{13,13}\.hdf$/)
{
    handleerror("Secondary Scene ID $secondaryid is not legal"); # exits
}

# make sure it is a valid MODIS dataset
if ($dataset =~ /^M\wD\d\d[\d\w]{1,2}(_\w{3,6})?$/)
{
    handleerror("Dataset $dataset is not legal"); #exits
}

# extract the components needed from the scene id to build the path to the 
# actual image
my @idFields = split(/\./, $secondaryid);
my $year = substr($idFields[1],1,4);
my $gridcol = substr($idFields[2],0,3);
my $gridrow = substr($idFields[2],3,3);

# build the directory name for this sensor/path/row/year
my $dirname = "modis/" . $dataset . "/" . $gridcol . "/" . $gridrow . 
              "/y" . $year;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
if ($taintedsceneid =~ /^\.?(\d{3}):(\d{6,10})$/)
{
    $sceneid = $2;
}
elsif ($taintedsceneid =~ /^(\d{6,10})$/)
{
    $sceneid = $1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal");
}

# unencode the browse number
$taintedbrowsenumber =~ s/%(..)/pack("c",hex($1))/ge;

# make sure the browse number is untainted
if ($taintedbrowsenumber =~ /^(\d{1,2})$/)
{
    $browsenumber = $1;
}
else
{
    #print header(), start_html("Browse Number Error"),
    #      p("Browse Number is not legal"),
    #      end_html(), "\n";
    #exit;

    # We need to handle the old style for a while, so just ignore the error
}
# if $browsenumber is 0, this is an old-style file, so leave $browsenumext
# empty, otherwise add underscore and browse number to file names
if ($browsenumber)
{
    $browsenumext = "_$browsenumber";
}

# NOTE:This is to always show the original browse image. If the image 
# dimensions were 240x290 the original image was renamed with the 
# .5km.jpg for the naming. Images with dimensions of 240x240 are kept 
# with the file naming of secondaryid.jpg.

# building the image name with the granule ID without the production date
my $image = $dirname . "/" . $idFields[0].".".$idFields[1].".".
            $idFields[2].".".$idFields[3]."$browsenumext.5km.jpg";

# if image name above does not exist, the naming of the image file is
# named with the secondaryid.jpg.
if (!-e $image)
{
    $image = $dirname . "/" . $secondaryid;
    $image =~ s/\.hdf/$browsenumext\.jpg/;
    if (!-e $image)
    {
        handleerror("cannot find image $image");
        exit; # just for good measure
    }
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
    my (@msgs) = @_; # for debugging - do not print details in prod
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

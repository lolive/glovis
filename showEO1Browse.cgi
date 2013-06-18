#!/usr/bin/perl -wT
# This cgi script builds a web page to show the full resolution browse of the 
# MODIS scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

my %dir = ("EO1_ALI_PUB" => "eo1/ali",
           "EO1_HYP_PUB" => "eo1/hyp",
           "GLS2005_EO1" => "gls/gls2005_eo1",
           "GLS2010_EO1" => "gls/gls2010_eo1"
          );

# get the scene id and dataset passed in
my $tainted_sceneid = param('scene_id');
my $tainted_dataset = param('dataset'); # might be empty

# make sure it is really a scene id for one of the L1 datasets and extract
# some needed info from the scene id, determine sensorpath based on dataset
my $sceneid;
my $imagename;
my $sensorpath;
my $path;
my $row;
my $year;

# make sure it is really an EO-1 ALI or Hyperion scene id
if ($tainted_sceneid =~ /^EO1[AH]\d{16}\w{9}$/)
{
    # extract the components needed from the scene id to build the path to the 
    # actual image
    $sceneid = $tainted_sceneid;
    $path = substr($sceneid,4,3);
    $row = substr($sceneid,7,3);
    $year = substr($sceneid,10,4);
    
    if ($tainted_dataset)
    {
        if ($dir{$tainted_dataset})
        {
            $sensorpath = $dir{$tainted_dataset};
            # For GLS2005_EO1 and GLS2010_EO1, raw browse name is formed
            # with the full scene ID and ending in .jpg
            $imagename = $sceneid . ".jpg";
        }
        else
        {
            handleerror("Unrecognized dataset $tainted_dataset");
        }
    }
    else
    {
        # No dataset given (original ALI/Hyp); determine the dataset by the ID
        my $sensor = substr($sceneid,3,1);
        $_ = $sensor;
        if (m/A/)
        {
            $sensor = "ali";
            $sensorpath = $dir{'EO1_ALI_PUB'};
        }
        else
        {
            $sensor = "hyp";
            $sensorpath = $dir{'EO1_HYP_PUB'};
        }

        # For plain EO-1 ALI and Hyperion, raw browse name is formed
        # without the A or H in the third position, and ending in .jpeg
        substr ($sceneid, 3,1) = "";
        $imagename = $sceneid . ".jpeg";
    }
}
else
{
    handleerror("Scene ID $tainted_sceneid is not legal"); # exits
}

# build the directory name for this sensor/path/row/year
my $dirname = $sensorpath . "/p" . $path . "/r" . $row . 
              "/y" . $year;

# building the image name with the scene ID without the letter indicating 
# sensor type
my $image = $dirname . "/" . $imagename;

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
    print start_html("USGS Global Visualization Viewer - Browse Error");
    print p("Error generating browse image page");
    #foreach (@messages)
    #{
    #    print p("$_");
    #}
    print end_html();
    exit;
}

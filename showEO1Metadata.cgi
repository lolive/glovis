#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

use strict;
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $sceneid = param('scene_id');

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# make sure it is really a scene id 
$_ = $sceneid;

# make sure it is really a EO1 scene id
if (!m/^EO1[AH]\d{16}\w{9}$/)
{
    handleerror("Scene ID $sceneid is not legal"); # exits
}

# extract the components needed from the scene id to build the path to the 
# actual image
my $sensor = substr($sceneid,3,1);
my $res = 0;
$_ = $sensor;
if (m/A/)
{
    $sensor = "ali";
    $res = "0240";
}
else
{
    $sensor = "hyp";
    $res = "0120";
}

my $path = substr($sceneid,4,3);
my $row = substr($sceneid,7,3);
my $year = substr($sceneid,10,4);

# build the directory name for this sensor/path/row/year
my $dirname = "eo1/" . $sensor . "/p" . $path . "/r" . $row . 
              "/y" . $year;

# Build the .meta file name
my $metafile = $dirname ."/".$sceneid."_".$res.".meta";

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;

// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 865;
    winWidth = 445;

    // adjust the size if Net 4
    if (isNS4 && !isNS6)
    {
        winHeight = winHeight - 190
        winWidth = winWidth - 20
    }
    // make sure we do not exceed screen size
    if (winHeight > screen.height)
        winHeight = screen.height
    if (winWidth > screen.width)
        winWidth = screen.width

    resizeTo(winWidth,winHeight);
}
function loadmeta(anchor,page) {
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winWidth=800
    winHeight=700
    if (winHeight > screen.height)
        winHeight = screen.height
    if (winWidth > screen.width)
        winWidth = screen.width

    if (isNS6 || isNS4)
    {
        win = open("", "glovismetadataglossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes,width=" + winWidth + ",height=" + winHeight);
    }
    else
    {
        win = open("", "glovismetadataglossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes");
    }
    
    win.location = page + anchor;
    win.focus();
}


// this function loads a new browser to show the FGDC metadata for the given
// scene.  It utilizes the EE FGDC metadata script
function loadfgdcmeta(page) {

    winWidth=800
    winHeight=700
    if (winHeight > screen.height)
        winHeight = screen.height
    if (winWidth > screen.width)
        winWidth = screen.width

    win = open(page, "_blank", "directories=yes,toolbar=yes,menubar=yes," +
        "scrollbars=yes,resizable=yes,location=yes,status=yes,width=" 
        + winWidth + ",height=" + winHeight);
    
    win.location = page;
    win.focus();
}
END

# build the web page
print header();
print start_html(-title=>"USGS Global Visualization Viewer - $sceneid Metadata",
                 -script=>$JSCRIPT, -onLoad=>'init()');
buildMetaTable($metafile);
addFgdcMetadata();
print end_html();
exit;

# routine to convert Y/N flags into a full word description
sub Yes_No
{
    $_ = $_[0];
    return "Yes" if (m/^[Y|y]/);
    return "No";
}

# routines to format a decimal degree value as a DMS value
# inputs: decimal degrees, Direction String (N/S/E/W)
sub format_DMS
{
    my $dd = $_[0];
    my $direction = $_[1];

    my $degrees = int($dd);
    my $dm = ($dd - $degrees) * 60; # get decimal minutes
    my $minutes = int($dm);
    # get seconds.  Don't bother rounding since the values aren't that 
    # accurate anyway.
    my $seconds = int(($dm - $minutes) * 60);
    my $dms = sprintf("%d&deg;%02d'%02d&quot;%s",$degrees,$minutes,$seconds,
                      $direction);
    return $dms;
}

# routine to format DMS values for Latitudes
sub format_DMS_lat
{
    my $dd = $_[0]; # get the decimal degrees
    return "" if (!defined($dd) || $dd =~ /^\s*$/);

    # set the N/S flag
    my $direction = "N";
    if ($dd < 0)
    {
        $direction = "S";
        $dd = -$dd;
    }
    # convert to a DMS string
    return format_DMS($dd,$direction);
}

# routine to format DMS values for Longitudes
sub format_DMS_lon
{
    my $dd = $_[0]; # get the decimal degrees
    return "" if (!defined($dd) || $dd =~ /^\s*$/);

    # set the E/W flag
    my $direction = "E";
    if ($dd < 0)
    {
        $direction = "W";
        $dd = -$dd;
    }
    # convert to a DMS string
    return format_DMS($dd,$direction);
}

# read the metadata from the file, format it nicely and according to
# the sensor, and print the html table
sub buildMetaTable
{
    my ($filename) = @_;
    my @order;
    my %glossaryLinks; # web page links;
    my %metaInfo;      # metadata as read from the file
    my %metadata;      # metadata formatted for display
                       # where keys match values in @order array

    # Metadata layout
    # This is the order we want the items to appear in the table
    @order = ("Entity ID",
              "Acquisition Date",
              "NW Corner",
              "NE Corner",
              "SW Corner",
              "SE Corner",
              "Image Cloud Cover",
              "Image Quality",
              "Receiving Station",
              "Scene Start Time",
              "Scene Stop Time",
              "Date Entered",
              "Target Path",
              "Target Row",
              "Orbit Path",
              "Orbit Row",
              "Browse Available",
              "Look Angle",
              "Sun Azimuth",
              "Sun Elevation",
              "Standard L1 Processing Level"
             );
                 
    # This is a hash holding the links to the web page        
    %glossaryLinks = ("Entity ID" => "entity_id",
                      "Acquisition Date" => "acquisition_date",
                      "NW Corner" => "nw_corner",
                      "NE Corner" => "ne_corner",
                      "SW Corner" => "sw_corner",
                      "SE Corner" => "se_corner",
                      "Image Cloud Cover" => "image_cloud_cover",
                      "Image Quality" => "image_quality",
                      "Receiving Station" => "receiving_station",
                      "Scene Start Time" => "scene_start_time",
                      "Scene Stop Time" => "scene_stop_time",
                      "Date Entered" => "date_entered",
                      "Target Path" => "target_path",
                      "Target Row" => "target_row",
                      "Orbit Path" => "orbit_path",
                      "Orbit Row" => "orbit_row",
                      "Browse Available" => "browse_available",
                      "Look Angle" => "look_angle",
                      "Sun Azimuth" => "sun_azimuth",
                      "Sun Elevation" => "sun_elevation",
                      "Standard L1 Processing Level" => "std_l1_process_level"
                     );
      
    # read the lines from the metadata file
    open (META,"<".$filename) or handleerror("Can't open $filename: $!");
    
    while (<META>)
    {
        chomp; 
        # There are no spaces in keys, so all non-space before the = is the key
        # and everything after the = (and optional whitespace) is the value
        if (/(\S+)\s*=\s*(.*)/)
        {
            $metaInfo{$1} = $2; # $2 could be empty
        }
        else
        {
            handleerror("Cannot parse metadata line: $_");
        }
    }

    # assign the values to the metadata hash that is used to populate the table

    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    @metadata{@order} = ("") x scalar(@order);
    
    $metadata{"Entity ID"} = $metaInfo{entityID};
  
    $metadata{"Image Quality"} = $metaInfo{Quality};

    $metadata{"Receiving Station"} = $metaInfo{RecStation};
   
    $metadata{"Scene Start Time"} = $metaInfo{SceneStartTime};

    $metadata{"Scene Stop Time"} = $metaInfo{SceneStopTime};

    $metadata{"Orbit Path"} = $metaInfo{OrbitPath};
  
    $metadata{"Orbit Row"} = $metaInfo{OrbitRow};
    
    $metadata{"Target Path"} = $metaInfo{path};
   
    $metadata{"Target Row"} = $metaInfo{row};

    $metadata{"Browse Available"} = Yes_No($metaInfo{BrowseAvail});
    
    # format the Acquisition Date value
    my $date = substr($metaInfo{AcqDate},0,4) . "/" .
               substr($metaInfo{AcqDate},4,2) . "/" .
               substr($metaInfo{AcqDate},6,2);

    $metaInfo{AcqDate} = $date;
    $metadata{"Acquisition Date"} = $metaInfo{AcqDate};

    # formate the Date Entered value
    $date = substr($metaInfo{DateEntered},0,4) . "/" .
            substr($metaInfo{DateEntered},4,2) . "/" .
            substr($metaInfo{DateEntered},6,2);

    $metaInfo{DateEntered} = $date;
    $metadata{"Date Entered"} = $metaInfo{DateEntered};

    my $tmp;

    # format ULLat coordinate values
    $tmp = format_DMS_lat($metaInfo{ULLat});
    $metaInfo{ULLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{ULLong});
    $metaInfo{ULLong} = $tmp;
    $metadata{"NW Corner"} = $metaInfo{ULLat}.", ".$metaInfo{ULLong};

    # format URLat coordinate values
    $tmp = format_DMS_lat($metaInfo{URLat});
    $metaInfo{URLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{URLong});
    $metaInfo{URLong} = $tmp;
    $metadata{"NE Corner"} = $metaInfo{URLat}.", ".$metaInfo{URLong};

    # format LLLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LLLat});
    $metaInfo{LLLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LLLong});
    $metaInfo{LLLong} = $tmp;
    $metadata{"SW Corner"} = $metaInfo{LLLat}.", ".$metaInfo{LLLong};

    # format LRLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LRLat});
    $metaInfo{LRLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LRLong});
    $metaInfo{LRLong} = $tmp;
    $metadata{"SE Corner"} = $metaInfo{LRLat}.", ".$metaInfo{LRLong};

    # include the cloud cover range
    if ($metaInfo{SceneCc} == -1)
    {
        # -1 cloud cover means nothing was in the database for the cloud cover,
        # so leave it blank
        $metadata{"Image Cloud Cover"} = "";
    }
    elsif ($metaInfo{SceneCc} == 90)
    {
        $metadata{"Image Cloud Cover"} = "90% to 100% Cloud Cover";
    }
    else
    {
        $metadata{"Image Cloud Cover"} = $metaInfo{SceneCc}."% to ".
                                 ($metaInfo{SceneCc} + 9)."% Cloud Cover";
    }

    # find look angle of scene
    $metadata{"Look Angle"} = $metaInfo{LookAngle};

    # find the sun azimuth value
    $metadata{"Sun Azimuth"} = $metaInfo{SunAzi};

    # find the sun elevation value
    $metadata{"Sun Elevation"} = $metaInfo{SunElev};

    # find the standard l1 processing level
    $metadata{"Standard L1 Processing Level"} = $metaInfo{StandardL1ProcessLevel};

    # name of the glossary pages used for EO-1
    my $page = "glossary_eo1.html#";
    
    # generate the table for the metadata
    print "<TABLE BORDER=\"1\" CELLPADDING=\"2\">\n";
    print Tr({-align=>"CENTER",-bgcolor=>"#006666"},
             td(font({-color=>"#FFFFCC"},"Dataset Attribute")),
             td(font({-color=>"#FFFFCC"},"Attribute Value")));
    foreach my $metaItem (@order)
    {   
        # if a $metaInfo{} key did not exist, $metadata{} value may be empty
        $metadata{$metaItem} = "" if (!defined($metadata{$metaItem})
                                            || $metadata{$metaItem} =~ /^\s*$/);

        # a check for the metaItem to see if it needs a link to the
        # glossary page.
        if (!exists($glossaryLinks{$metaItem}))
        {
            print "<tr><td>$metaItem</td>";
            print "<td>$metadata{$metaItem}</td></tr>\n";
        }
        else
        {
            print "<tr><td><a href=\"#\" ";
            print "onClick=\"loadmeta('$glossaryLinks{$metaItem}','$page');\">";
            print "$metaItem</a></td>";
            print "<td>$metadata{$metaItem}</td></tr>\n";
        }
    }
    print "\n</TABLE>\n";

    
    close (META);
}

# routine to generate an error page  
sub handleerror
{
    my @msgs = @_; # For debug - do not print details in prod
    print header();
    print start_html("USGS Global Visualization Viewer - Metadata Error");
    print p("Error generating metadata page");
    #foreach (@msgs)
    #{
    #    print p($_);
    #}
    print end_html();
    exit;
}

# routine to add link to FGDC metadata
sub addFgdcMetadata
{
    my $collection_id;

    if ($sensor eq "ali")
    {
        $collection_id = '1852';
    }
    elsif ($sensor eq "hyp")
    {
        $collection_id = '1854';
    }
    
    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('http://earthexplorer.usgs.gov/form/fgdcmetadatalookup?collection_id=$collection_id&entity_id=$sceneid&primary_key=$sceneid&pageView=1');\">";
    print "FGDC Metadata</a></p>";
}

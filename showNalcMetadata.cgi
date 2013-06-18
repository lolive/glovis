#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the NALC scene id
# passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);
use File::Find;

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
    handleerror("Scene ID $taintedsceneid is not legal"); # exits
}

# extract the path and row from the scene id
my $path = substr($sceneid, 6, 3);
my $row = substr($sceneid, 9, 3);
if (($path < 1) || ($path > 233) || ($row < 1) || ($row > 124))
{
    handleerror("Scene ID $taintedsceneid is not legal"); # exits
}

# build the directory name for this sensor/path/row
my $dirname = "nalc/p$path/r$row";

# build the metadata filename
my $metafile = $dirname . "/" . $sceneid .".meta";

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;
// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 840;
    winWidth = 500;

    // adjust the size if Net 4
    if (isNS4 && !isNS6)
    {
        winHeight = winHeight - 165
        winWidth = winWidth - 15
    }
    // make sure we do not exceed screen size
    if (winHeight > screen.height)
        winHeight = screen.height
    if (winWidth > screen.width)
        winWidth = screen.width

    resizeTo(winWidth,winHeight);
}
function loadmeta(anchor) {
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
    win.location = "glossary_nalc.html#" + anchor;
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

    win = open(page, "_blank", "directories=yes,toolbar=yes,menubar=yes" +
        ",scrollbars=yes,resizable=yes,location=yes,status=yes,width=" + 
        winWidth + ",height=" + winHeight);

    win.location = page;
    win.focus();
}
END

# build the web page
print header();
print start_html(-title=>"USGS Global Visualization Viewer - $sceneid Metadata",
                 -script=>$JSCRIPT, -onLoad=>'init()');
buildMetaTable($metafile,$path,$row);
addFgdcMetadata();
print end_html();
exit;

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

# routine to format the cloud cover value
sub format_cloud_cover
{
    my $cloud_cover = $_[0];
    my $cc = $cloud_cover;
    return $cc if ($cc eq "");
    if (($cloud_cover >= 0) && ($cloud_cover <= 9))
    {
        $cloud_cover = int($cloud_cover);
        $cc = ($cloud_cover * 10) . "% to " . (($cloud_cover + 1) * 10 - 1)
            . "% Cloud Cover";
    }
    return $cc;
}

# routine to format the processing level
sub format_processing_level
{
    my $level = $_[0];
    return "Composite" if ($level eq "C");
    return "DEM Extracted" if ($level eq "E");
    return "Geocoded/Georegistered" if ($level eq "G");
    return "Terrain Corrected/Georegistered" if ($level eq "T");
    return $level;
}

# routine to format the resampling technique
sub format_resampling_tech
{
    my $tech = $_[0];
    return "Cubic Convolution" if ($tech eq "CC");
    return "Bilinear" if ($tech eq "BI");
    return $tech;
}

# routine to format the map projection
sub format_map_projection
{
    my $proj = $_[0];
    return "Universal Transverse Mercator" if ($proj eq "U");
    return $proj;
}

# routine to read the metadata and build the html table
sub buildMetaTable
{
    my ($filename,$path,$row) = @_;

    my %metaInfo;   # A hash holding the metadata names;
    my %metadata;   # A hash holding the metadata names; 

    # This is the order we want the items to appear in the table
    my @order = (
                "Entity ID",
                "WRS Path",
                "WRS Row",
                "NW Corner",
                "NE Corner",
                "SW Corner",
                "SE Corner",
                "Center Coordinates",
                "Map Projection Code",
                "Resampling Technique",
                "Scene ID for first scene of decade 70",
                "Acquisition Date for first scene of Decade 70",
                "Cloud cover of first scene in decade 70",
                "Processing level of first scene decade 70",
                "Scene ID for second scene of decade 70",
                "Acquisition Date for second scene of Decade 70",
                "Cloud cover of second scene in decade 70",
                "Processing level of second scene decade 70",
                "Scene ID for third scene of decade 70",
                "Acquisition Date for third scene of Decade 70",
                "Cloud cover of third scene in decade 70",
                "Processing level of third scene decade 70",
                "Scene ID for third scene of decade 70",
                "Acquisition Date for third scene of Decade 70",
                "Cloud cover of third scene in decade 70",
                "Scene ID for first scene of decade 80",
                "Acquisition Date for first scene of Decade 80",
                "Cloud cover of first scene in decade 80",
                "Processing level of first scene decade 80",
                "Scene ID for second scene of decade 80",
                "Acquisition Date for second scene of Decade 80",
                "Cloud cover of second scene in decade 80",
                "Processing level of second scene decade 80",
                "Scene ID for first scene of decade 90",
                "Acquisition Date for first scene of Decade 90",
                "Cloud cover of first scene in decade 90",
                "Processing level of first scene decade 90",
                "Scene ID for second scene of decade 90",
                "Acquisition Date for second scene of Decade 90",
                "Cloud cover of second scene in decade 90",
                "Processing level of second scene decade 90",
                "Processing Level of DEM",
                "Resampling Technique DEM",
                "Browse Availability"
                );
    
   my %glossaryLinks = (
                "Entity ID" => "entity_id",
                "WRS Path" => "path_nbr",
                "WRS Row" => "row_nbr",
                "NW Corner" => "ul_corner_lat",
                "NE Corner" => "ur_corner_lat",
                "SW Corner" => "ll_corner_lat",
                "SE Corner" => "lr_corner_lat",
                "Scene Center" => "scene_center_lat",
                "Map Projection Code" => "map_projection_code",
                "Resampling Technique" => "resampling_tech",
                "Scene ID for first scene of decade 70" => "scene_id",
                "Acquisition Date for first scene of Decade 70" => "acq_date",
                "Cloud cover of first scene in decade 70" => "cloud_cover",
                "Processing level of first scene decade 70" => "proc_level",
                "Scene ID for second scene of decade 70" => "scene_id",
                "Acquisition Date for second scene of Decade 70" => "acq_date",
                "Cloud cover of second scene in decade 70" => "cloud_cover",
                "Processing level of second scene decade 70" => "proc_level",
                "Scene ID for third scene of decade 70" => "scene_id",
                "Acquisition Date for third scene of Decade 70" => "acq_date",
                "Cloud cover of third scene in decade 70" => "cloud_cover",
                "Processing level of third scene decade 70" => "proc_level",
                "Scene ID for third scene of decade 70" => "scene_id",
                "Acquisition Date for third scene of Decade 70" => "acq_date",
                "Cloud cover of third scene in decade 70" => "cloud_cover",
                "Scene ID for first scene of decade 80" => "scene_id",
                "Acquisition Date for first scene of Decade 80" => "acq_date",
                "Cloud cover of first scene in decade 80" => "cloud_cover",
                "Processing level of first scene decade 80" => "proc_level",
                "Scene ID for second scene of decade 80" => "scene_id",
                "Acquisition Date for second scene of Decade 80" => "acq_date",
                "Cloud cover of second scene in decade 80" => "cloud_cover",
                "Processing level of second scene decade 80" => "proc_level",
                "Scene ID for first scene of decade 90" => "scene_id",
                "Acquisition Date for first scene of Decade 90" => "acq_date",
                "Cloud cover of first scene in decade 90" => "cloud_cover",
                "Processing level of first scene decade 90" => "proc_level",
                "Scene ID for second scene of decade 90" => "scene_id",
                "Acquisition Date for second scene of Decade 90" => "acq_date",
                "Cloud cover of second scene in decade 90" => "cloud_cover",
                "Processing level of second scene decade 90" => "proc_level",
                "Processing Level of DEM" => "proc_level",
                "Resampling Technique DEM" => "resampling_tech_dem",
                "Browse Availability" => "browse_avail_flag"
                );

    
    # read the lines from the metadata file
    open (META,"<".$filename) || handleerror("Can't open $filename: $!");
    
    while (<META>)
    {
        chomp;
        my ($name,$value) = split /\s*=\s*/;
        $metaInfo{$name} = $value;
    }

    close (META);

    # assign the values to the metadata hash that is used to populate the table

    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    @metadata{@order} = ("") x scalar(@order);

    $metadata{"Entity ID"} = $metaInfo{entityID};
    $metadata{"WRS Path"} = int($path);
    $metadata{"WRS Row"} = int($row);
   
    my $tmp;

    #format ULLat coordinate values
    $tmp = format_DMS_lat($metaInfo{ULLat});
    $metaInfo{ULLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{ULLong});
    $metaInfo{ULLong} = $tmp;
    $metadata{"NW Corner"} = $metaInfo{ULLat}.", ". $metaInfo{ULLong};

    #format URLat coordinate values
    $tmp = format_DMS_lat($metaInfo{URLat});
    $metaInfo{URLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{URLong});
    $metaInfo{URLong} = $tmp;
    $metadata{"NE Corner"} = $metaInfo{URLat}.", ".  $metaInfo{URLong};

    #format LRLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LRLat});
    $metaInfo{LRLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LRLong});
    $metaInfo{LRLong} = $tmp;
    $metadata{"SW Corner"} = $metaInfo{LRLat}.", ". $metaInfo{LRLong};

    #format LLLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LLLat});
    $metaInfo{LLLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LLLong});
    $metaInfo{LLLong} = $tmp;
    $metadata{"SE Corner"} = $metaInfo{LLLat}.", ". $metaInfo{LLLong};

    # format the Scene Center coordinate values
    $tmp = format_DMS_lat($metaInfo{scene_center_lat});
    $metaInfo{scene_center_lat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{scene_center_lon});
    $metaInfo{scene_center_lon} = $tmp;
    $metadata{"Center Coordinates"} = $metaInfo{scene_center_lat}.", "
                                    . $metaInfo{scene_center_lon};

    $metadata{"Map Projection Code"}
                = format_map_projection($metaInfo{MapProjectionCode});
    $metadata{"Resampling Technique"}
                = format_resampling_tech($metaInfo{ResamplingTech});

    $metadata{"Scene ID for first scene of decade 70"} = $metaInfo{SceneId70_1};
    $metadata{"Acquisition Date for first scene of Decade 70"}
                = $metaInfo{AcqDate70_1};
    $metadata{"Cloud cover of first scene in decade 70"}
                = format_cloud_cover($metaInfo{SceneCc70_1});
    $metadata{"Processing level of first scene decade 70"}
                = format_processing_level($metaInfo{ProcLevel70_1});

    $metadata{"Scene ID for second scene of decade 70"}
                = $metaInfo{SceneId70_2};
    $metadata{"Acquisition Date for second scene of Decade 70"}
                = $metaInfo{AcqDate70_2};
    $metadata{"Cloud cover of second scene in decade 70"}
                = format_cloud_cover($metaInfo{SceneCc70_2});
    $metadata{"Processing level of second scene decade 70"}
                = format_processing_level($metaInfo{ProcLevel70_2});

    $metadata{"Scene ID for third scene of decade 70"} = $metaInfo{SceneId70_3};
    $metadata{"Acquisition Date for third scene of Decade 70"}
                = $metaInfo{AcqDate70_3};
    $metadata{"Cloud cover of third scene in decade 70"}
                = format_cloud_cover($metaInfo{SceneCc70_3});
    $metadata{"Processing level of third scene decade 70"}
                = format_processing_level($metaInfo{ProcLevel70_3});
    
    $metadata{"Scene ID for first scene of decade 80"} = $metaInfo{SceneId80_1};
    $metadata{"Acquisition Date for first scene of Decade 80"}
                = $metaInfo{AcqDate80_1};
    $metadata{"Cloud cover of first scene in decade 80"}
                = format_cloud_cover($metaInfo{SceneCc80_1});
    $metadata{"Processing level of first scene decade 80"}
                = format_processing_level($metaInfo{ProcLevel80_1});

    $metadata{"Scene ID for second scene of decade 80"}
                = $metaInfo{SceneId80_2};
    $metadata{"Acquisition Date for second scene of Decade 80"}
                = $metaInfo{AcqDate80_2};
    $metadata{"Cloud cover of second scene in decade 80"}
                = format_cloud_cover($metaInfo{SceneCc80_2});
    $metadata{"Processing level of second scene decade 80"}
                = format_processing_level($metaInfo{ProcLevel80_2});

    $metadata{"Scene ID for first scene of decade 90"} = $metaInfo{SceneId90_1};
    $metadata{"Acquisition Date for first scene of Decade 90"}
                = $metaInfo{AcqDate90_1};
    $metadata{"Cloud cover of first scene in decade 90"}
                = format_cloud_cover($metaInfo{SceneCc90_1});
    $metadata{"Processing level of first scene decade 90"}
                = format_processing_level($metaInfo{ProcLevel90_1});

    $metadata{"Scene ID for second scene of decade 90"}
                = $metaInfo{SceneId90_2};
    $metadata{"Acquisition Date for second scene of Decade 90"}
                = $metaInfo{AcqDate90_2};
    $metadata{"Cloud cover of second scene in decade 90"}
                = format_cloud_cover($metaInfo{SceneCc90_2});
    $metadata{"Processing level of second scene decade 90"}
                = format_processing_level($metaInfo{ProcLevel90_2});

    $metadata{"Processing Level of DEM"}
                = format_processing_level($metaInfo{ProcLevelDem});
    $metadata{"Resampling Technique DEM"}
                = format_resampling_tech($metaInfo{ResamplingTechDem});
    $metadata{"Browse Availability"} = $metaInfo{BrowseAvail};

    my $page = "glossary_nalc.html#";

    # generate the table for the metadata
    print "<TABLE BORDER=\"1\" CELLPADDING=\"2\">\n";
    print Tr({-align=>"CENTER",-bgcolor=>"#006666"},
             td(font({-color=>"#FFFFCC"},"Dataset Attribute")),
             td(font({-color=>"#FFFFCC"},"Attribute Value")));
    foreach my $metaItem (@order)
    {   
        print "<tr>\n";
        print td("<a href=\"#\" "
                . "onClick=\"loadmeta('$glossaryLinks{$metaItem}','$page');\">"
                . "$metaItem</a>");
        print td($metadata{$metaItem}) . "</tr>\n";
    }
    print "\n</TABLE>\n";
}

# routine to add link to FGDC metadata
sub addFgdcMetadata
{
    my $collection_id = '2671';
    my $id = substr($sceneid,8);
    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('http://earthexplorer.usgs.gov/form/fgdcmetadatalookup?collection_id=$collection_id&entity_id=$id&primary_key=$id&pageView=1');\">";
    print "FGDC Metadata</a></p>";
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


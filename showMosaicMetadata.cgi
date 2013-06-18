#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the Tri-decadal
# ETM+ mosaic scene id passed in

use strict;
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

# flag to indicate the directories may need to be searched for the correct
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
    handleerror("SceneID $taintedsceneid is not legal");
}

my $column;
my $row;
my $metafile;
if ($sceneid =~ /^MT/)
{
    # TM mosaic.  find the column and row from the scene id
    $column = substr($sceneid, 4, 2);
    $row = substr($sceneid, 2, 1) . substr($sceneid, 7, 2);

    # build the metadata filename
    $metafile = "ortho/tm_mosaic/$column/$row/$sceneid.meta";

    # if we need to search for the TM mosaic, if it doesn't exist at the 
    # current location, check the alternate row
    if (($search_for_TM) && ! -e $metafile)
    {
        $row = substr($sceneid, 2, 1) . substr($sceneid, 10, 2);
        $metafile = "ortho/tm_mosaic/$column/$row/$sceneid.meta";
    }
}
else
{
    # ETM mosaic.  find the column and row from the scene id
    $column = substr($sceneid, 4, 2) . substr($sceneid, 11, 1);
    $row = substr($sceneid, 2, 1) . substr($sceneid, 7, 2)
         . substr($sceneid, 10, 1);

    # build the metadata filename
    $metafile = "ortho/etm_mosaic/$column/$row/$sceneid.meta";
}

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;
// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 882;
    winWidth = 300;

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
    win.location = "glossary_mosaic.html#" + anchor;
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
buildMetaTable($metafile,$column,$row);
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

# routine to format a date in YYYYMMDD format to YYYY/MM/DD format
sub format_date
{
    my $in = $_[0];
    my $date = $in;

    if ($in =~ /^(\d{4})(\d{2})(\d{2})$/)
    {
        $date = $1 . "/" . $2 . "/" . $3;
    }

    return $date;
}

# routine to read the metadata and build the html table
sub buildMetaTable
{
    my ($filename, $column, $row) = @_;

    my %metaInfo;   # metadata as read from the file
    my %metadata;   # metadata formatted for display
                    # where keys match values in the @order array

    # This is the order we want the items to appear in the table
    my @order = (
                "Entity ID",
                "File Format",
                "Platform",
                "Sensor",
                "Starting Row",
                "Starting Column",
                "Row Count",
                "Column Count",
                "Datum",
                "Units",
                "X Start",
                "Y Start",
                "X Increment",
                "Y Increment",
                "NW Corner",
                "NE Corner",
                "SW Corner",
                "SE Corner",
                "Center Coordinates",
                "Component Count",
                "Earliest Acq Date",
                "Latest Acq Date",
                "UTM Zone"
                );
    
   my %glossaryLinks = (
                "Entity ID" => "entity_id",
                "File Format" => "file_format",
                "Platform" => "platform",
                "Sensor" => "sensor",
                "Starting Row" => "starting_row",
                "Starting Column" => "starting_column",
                "Row Count" => "row_count",
                "Column Count" => "column_count",
                "Datum" => "datum",
                "Units" => "units",
                "X Start" => "x_start",
                "Y Start" => "y_start",
                "X Increment" => "x_increment",
                "Y Increment" => "y_increment",
                "NW Corner" => "nw_corner",
                "NE Corner" => "ne_corner",
                "SW Corner" => "sw_corner",
                "SE Corner" => "se_corner",
                "Center Coordinates" => "center_coordinates",
                "Component Count" => "component_count",
                "Earliest Acq Date" => "earliest_acq_date",
                "Latest Acq Date" => "latest_acq_date",
                "UTM Zone" => "utm_zone"
                );

    
    # read the lines from the metadata file
    open (META,"<".$filename) || handleerror("Can't open $filename: $!");

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

    close (META);

    # assign the values to the metadata hash that is used to populate the table

    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    @metadata{@order} = ("") x scalar(@order);

    $metadata{"Entity ID"} = $metaInfo{entityID};
    $metadata{"File Format"} = $metaInfo{FileFormat};
    $metadata{"Platform"} = $metaInfo{Platform};
    $metadata{"Sensor"} = $metaInfo{Sensor};
    $metadata{"Starting Row"} = $metaInfo{RowStart};
    $metadata{"Starting Column"} = $metaInfo{ColStart};
    $metadata{"Row Count"} = $metaInfo{RowCount};
    $metadata{"Column Count"} = $metaInfo{ColCount};
    $metadata{"Datum"} = $metaInfo{Datum};
    $metadata{"Units"} = $metaInfo{Units};
    $metadata{"X Start"} = $metaInfo{XStart};
    $metadata{"Y Start"} = $metaInfo{YStart};
    $metadata{"X Increment"} = $metaInfo{XIncrement};
    $metadata{"Y Increment"} = $metaInfo{YIncrement};
    $metadata{"Component Count"} = $metaInfo{ComponentCount};
    $metadata{"UTM Zone"} = $metaInfo{UtmZone};

    $metadata{"NW Corner"} = format_DMS_lat($metaInfo{ULLat}) . ", "
                           . format_DMS_lon($metaInfo{ULLong});
    $metadata{"NE Corner"} = format_DMS_lat($metaInfo{URLat}) . ", "
                           . format_DMS_lon($metaInfo{URLong});
    $metadata{"SW Corner"} = format_DMS_lat($metaInfo{LLLat}) . ", "
                           . format_DMS_lon($metaInfo{LLLong});
    $metadata{"SE Corner"} = format_DMS_lat($metaInfo{LRLat}) . ", "
                           . format_DMS_lon($metaInfo{LRLong});
    $metadata{"Center Coordinates"} = format_DMS_lat($metaInfo{SCLat}) . ", "
                           . format_DMS_lon($metaInfo{SCLong});

    $metadata{"Earliest Acq Date"} = format_date($metaInfo{EarliestAcqDate});
    $metadata{"Latest Acq Date"} = format_date($metaInfo{LatestAcqDate});

    my $page = "glossary_mosaic.html#";

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
    my $collection_id = '';
    if ($sceneid =~ /^MT/)
    {
        # TM mosaic.
        $collection_id = '3492';
    }
    else
    {
        # ETM+ Pan Mosaic.
        $collection_id = '3491';
    }
    my $id = $sceneid;
    print "<p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('http://earthexplorer.usgs.gov/form/fgdcmetadatalookup?collection_id=$collection_id&entity_id=$id&primary_key=$id&pageView=1');\">";
    print "FGDC Metadata</a></p>";
}

# routine to generate an error page
sub handleerror
{
    my @messages = @_; # mainly for debugging - don't print details in prod

    print header(),
          start_html("USGS Global Visualization Viewer - Scene $sceneid"),
          p("Error generating metadata page");

    # Un-comment this for debugging
    #foreach(@messages)
    #{
    #    print p($_);
    #}
    print end_html();
    exit;
}


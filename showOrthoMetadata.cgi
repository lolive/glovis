#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the
# scene id passed in

use strict;
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $sceneid;
my $path;
my $row;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
my $platform;
my $sat_num;
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
    handleerror("Scene ID $taintedsceneid is not legal"); # exits
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

my $acqDate;
my $NBound;
my $SBound;
my $WBound;
my $EBound;

# build the metadata file name for this sensor/path/row/year/sceneid
my $dirname;
$dirname = "ortho/tm" if ($platform eq "tm");
$dirname = "ortho/etm" if ($platform eq "etm");
$dirname = "ortho/pansharp_etm" if ($platform eq "etmpan");
$dirname = "ortho/mss1_3" if ($platform eq "mss1_3");
$dirname = "ortho/mss4_5" if ($platform eq "mss4_5");
$dirname .= "/p" . $path . "/r" . $row;
my $year;
if (($platform eq "tm") || ($platform eq "mss1_3") || ($platform eq "mss4_5"))
{
   $year = substr($sceneid,12,4);
}
else
{
   $year = substr($sceneid,13,4);
}
my $metafile = "$dirname/y$year/$sceneid.meta";

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;
// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 740;
    winWidth = 370;

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
function load_ortho_tm_meta(anchor) {
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
        win = open("", "glovismetadata_ortho_tm_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes,width=" + winWidth + ",height=" + winHeight);
    }
    else
    {
        win = open("", "glovismetadata_ortho_tm_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes");
    }
    win.location = "glossary_ortho_tm.html#" + anchor;
    win.focus();
}

function load_ortho_mss1_3_meta(anchor) {
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
        win = open("", "glovismetadata_ortho_mss1_3_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes,width=" + winWidth + ",height=" + winHeight);
    }
    else
    {
        win = open("", "glovismetadata_ortho_mss1_3_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes");
    }
    win.location = "glossary_ortho_mss1_3.html#" + anchor;
    win.focus();
}

function load_ortho_mss4_5_meta(anchor) {
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
        win = open("", "glovismetadata_ortho_mss4_5_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes,width=" + winWidth + ",height=" + winHeight);
    }
    else
    {
        win = open("", "glovismetadata_ortho_mss4_5_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes");
    }
    win.location = "glossary_ortho_mss4_5.html#" + anchor;
    win.focus();
}

function load_ortho_pan_etm_meta(anchor) {
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
        win = open("", "glovismetadata_ortho_pan_etm_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes,width=" + winWidth + ",height=" + winHeight);
    }
    else
    {
        win = open("", "glovismetadata_ortho_pan_etm_glossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes");
    }
    win.location = "glossary_ortho_pan_etm.html#" + anchor;
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

sub buildMetaTable
{
    my ($filename,$path,$row) = @_;

    my %metaInfo;
    my %metadata;   # This is a hash of arrays. Index 0 holds the metadata
                    # value; index 1 holds the anchor name for the glossary

    # This is the order we want the items to appear in the table
    my @order;
    if (($platform eq "tm") || ($platform eq "mss4_5"))
    {
        @order = (
                "Entity ID",
                "Scene Size",
                "Acquisition Date",
                "Scene Cloud Cover",
                "WRS-2 Path",
                "WRS-2 Row",
                "Zone",
                "Upper Left Corner",
                "Upper Right Corner",
                "Lower Left Corner",
                "Lower Right Corner",
                "Scene Center",
                "Sun Azimuth",
                "Sun Elevation",
                "Day or Night",
                "Browse Available"
                 );
    }
    elsif ($platform eq "mss1_3")
    {
        @order = (
                "Entity ID",
                "Scene Size",
                "Acquisition Date",
                "Scene Cloud Cover",
                "WRS-1 Path",
                "WRS-1 Row",
                "Zone",
                "Upper Left Corner",
                "Upper Right Corner",
                "Lower Left Corner",
                "Lower Right Corner",
                "Scene Center",
                "Sun Azimuth",
                "Sun Elevation",
                "Day or Night",
                "Browse Available"
                 );
    }
    else
    {
        @order = (
                "Entity ID",
                "Scene Size",
                "Acquisition Date",
                "Scene Cloud Cover",
                "Pan Sharpened",
                "WRS-2 Path",
                "WRS-2 Row",
                "Zone",
                "Upper Left Corner",
                "Upper Right Corner",
                "Lower Left Corner",
                "Lower Right Corner",
                "Scene Center",
                "Sun Azimuth",
                "Sun Elevation",
                "Browse Available"
                 );
    }

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

    # add the path, row, and browse available to the metaInfo hash
    $metaInfo{path} = $path;
    $metaInfo{row} = $row;
    $metaInfo{avail} = "Yes";

    # get values needed to pass to the FGDC cgi script
    $acqDate = substr($metaInfo{AcqDate},5,2)."-".
               substr($metaInfo{AcqDate},8,2)."-".
               substr($metaInfo{AcqDate},0,4);
    # handle the different versions of metadata files
    if (defined $metaInfo{NBound})
    {
        $NBound = $metaInfo{NBound};
        $SBound = $metaInfo{SBound};
        $WBound = $metaInfo{WBound};
        $EBound = $metaInfo{EBound};
    }
    else
    {
        $NBound = $metaInfo{ULLat};
        $SBound = $metaInfo{LRLat};
        $WBound = $metaInfo{LLLong};
        $EBound = $metaInfo{URLong};
    }
    
    # assign the values to the metadata hash that is used to populate the table
    # for each value, the [0]th element is the value and the [1]st element
    # is the name of the anchor in the glossary page, used for linking
    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    foreach (@order) {
        $metadata{$_}[0] = "";
        $metadata{$_}[1] = "";
    }

    $metadata{"Entity ID"}[0] = $metaInfo{EntityID};
    $metadata{"Entity ID"}[1] = "entity_id";
    $metaInfo{GranSz} .= " MB";
    $metadata{"Scene Size"}[0] = $metaInfo{GranSz};
    $metadata{"Scene Size"}[1] = "scene_size";
    # format the date value
    substr($metaInfo{AcqDate},4,1,"/");
    substr($metaInfo{AcqDate},7,1,"/");
    $metadata{"Acquisition Date"}[0] = $metaInfo{AcqDate};
    $metadata{"Acquisition Date"}[1] = "acquisition_date";
    if (($platform eq "etm") || ($platform eq "etmpan"))
    {
        $metadata{"Pan Sharpened"}[0] = $metaInfo{PanSharp};
        $metadata{"Pan Sharpened"}[1] = "pan_sharp";
    }
    # format cloud cover value
    $metaInfo{SceneCc} .= "%";
    $metadata{"Scene Cloud Cover"}[0] = $metaInfo{SceneCc};
    $metadata{"Scene Cloud Cover"}[1] = "cloud_cover";
    if ($platform eq "mss1_3")
    {
        $metadata{"WRS-1 Path"}[0] = $metaInfo{path};
        $metadata{"WRS-1 Path"}[1] = "wrs_path";
        $metadata{"WRS-1 Row"}[0] = $metaInfo{row};
        $metadata{"WRS-1 Row"}[1] = "wrs_row";
    }
    else
    {
        $metadata{"WRS-2 Path"}[0] = $metaInfo{path};
        $metadata{"WRS-2 Path"}[1] = "wrs_path";
        $metadata{"WRS-2 Row"}[0] = $metaInfo{row};
        $metadata{"WRS-2 Row"}[1] = "wrs_row";
    }
    $metadata{"Zone"}[0] = $metaInfo{UTMzone};
    $metadata{"Zone"}[1] = "zone";
    # format the coordinate values
    my $tmp = format_DMS_lat($metaInfo{ULLat});
    $metaInfo{ULLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{ULLong});
    $metaInfo{ULLong} = $tmp;
    $tmp = format_DMS_lat($metaInfo{URLat});
    $metaInfo{URLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{URLong});
    $metaInfo{URLong} = $tmp;
    $tmp = format_DMS_lat($metaInfo{LRLat});
    $metaInfo{LRLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LRLong});
    $metaInfo{LRLong} = $tmp;
    $tmp = format_DMS_lat($metaInfo{LLLat});
    $metaInfo{LLLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LLLong});
    $metaInfo{LLLong} = $tmp;
    $metadata{"Upper Left Corner"}[0] = $metaInfo{ULLat}.", ".$metaInfo{ULLong};
    $metadata{"Upper Left Corner"}[1] = "ul_corner_lat";
    $metadata{"Upper Right Corner"}[0] = $metaInfo{URLat}.", ".
                                           $metaInfo{URLong};
    $metadata{"Upper Right Corner"}[1] = "ur_corner_lat";
    $metadata{"Lower Right Corner"}[0] = $metaInfo{LRLat}.", ".
                                          $metaInfo{LRLong};
    $metadata{"Lower Right Corner"}[1] = "lr_corner_lat";
    $metadata{"Lower Left Corner"}[0] = $metaInfo{LLLat}.", ".$metaInfo{LLLong};
    $metadata{"Lower Left Corner"}[1] = "ll_corner_lat";
    # format the center coordinate values
    $tmp = format_DMS_lat($metaInfo{scene_center_lat});
    $metaInfo{scene_center_lat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{scene_center_lon});
    $metaInfo{scene_center_lon} = $tmp;
    $metadata{"Scene Center"}[0] = $metaInfo{scene_center_lat}.", "
                                 .$metaInfo{scene_center_lon};
    $metadata{"Scene Center"}[1] = "scene_center_lat";
    $metadata{"Sun Azimuth"}[0] = $metaInfo{SunAzi};
    $metadata{"Sun Azimuth"}[1] = "sun_azimuth";
    $metadata{"Sun Elevation"}[0] = $metaInfo{SunElev};
    $metadata{"Sun Elevation"}[1] = "sun_elevation";
    if (($platform eq "tm") || ($platform eq "mss1_3") ||
        ($platform eq "mss4_5"))
    {
        $metadata{"Day or Night"}[0] = $metaInfo{DayNight};
        $metadata{"Day or Night"}[1] = "day_night";
    }
    $metadata{"Browse Available"}[0] = $metaInfo{avail};
    $metadata{"Browse Available"}[1] = "browse_avail";

    # generate the table for the metadata
    print "<CENTER>\n";
    print "<TABLE BORDER=\"1\" CELLPADDING=\"2\">\n";
    print Tr({-align=>"CENTER",-bgcolor=>"#006666"},
             td(font({-color=>"#FFFFCC"},"Dataset Attribute")),
             td(font({-color=>"#FFFFCC"},"Attribute Value")));
    foreach my $metaItem (@order)
    {
        # if a $metaInfo{} key did not exist, $metadata{}[0] value may be empty
        $metadata{$metaItem}[0] = ""
            if (!defined($metadata{$metaItem}[0])
                      || $metadata{$metaItem}[0] =~ /^\s*$/);

        print "<tr><td><a href=\"#\" ";
        if ($platform eq "tm")
        {
            print "onClick=\"load_ortho_tm_meta('$metadata{$metaItem}[1]');\">";
        }
        elsif ($platform eq "mss1_3")
        {
            print "onClick=\"load_ortho_mss1_3_meta".
                  "('$metadata{$metaItem}[1]');\">";
        }
        elsif ($platform eq "mss4_5")
        {
            print "onClick=\"load_ortho_mss4_5_meta".
                  "('$metadata{$metaItem}[1]');\">";
        }
        else
        {
            print "onClick=\"load_ortho_pan_etm_meta".
                  "('$metadata{$metaItem}[1]');\">";
        }
        print "$metaItem</a></td>";
        print "<td>$metadata{$metaItem}[0]</td></tr>\n";
    }
    print "\n</TABLE>\n";
    print "</CENTER>\n";
}

# routine to add link to FGDC metadata
sub addFgdcMetadata
{
    my $dataset;
    my $tmp;

    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('OrthoFGDC.cgi?scene_id=$sceneid&path=$path&row=$row&acq_date=$acqDate&NBound=$NBound&SBound=$SBound&WBound=$WBound&EBound=$EBound');\">";
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

#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the
# scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);
use File::Find;

# a few values change depending on the dataset
my %asterinfo = (
    "ASTER_VNIR" => {"subdir" => "aster/vnir",
                     "label"  => "Level 1A Scene ID",
                     "key"    => "L1A_dbID",
                     "anchor" => "l1a_scene_id",
                     "has_aster_granule_id" => 1},
    "ASTER_TIR"  => {"subdir" => "aster/tir",
                     "label"  => "Level 1A Scene ID",
                     "key"    => "L1A_dbID",
                     "anchor" => "l1a_scene_id",
                     "has_aster_granule_id" => 1},
    "ASTER_VNIR_DATAPOOL" => {"subdir" => "aster_datapool/vnir",
                     "label"  => "Level 1B Scene ID",
                     "key"    => "L1B_dbID",
                     "anchor" => "l1b_scene_id",
                     "has_aster_granule_id" => 0},
    "ASTER_TIR_DATAPOOL" => {"subdir" => "aster_datapool/tir",
                     "label"  => "Level 1B Scene ID",
                     "key"    => "L1B_dbID",
                     "anchor" => "l1b_scene_id",
                     "has_aster_granule_id" => 0}
);

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $tainteddataset = param('dataset');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $sceneid;
my $path;
my $row;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# some values vary based on dataset
my $subdir;
my $label;
my $key;
my $anchor;
my $has_aster_granule_id;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
if ($taintedsceneid =~ /^(\d{3}):(\d{5,10})$/)
{
    $sceneid = $2;
}
elsif ($taintedsceneid =~ /^(\d{5,10})$/)
{
    $sceneid = $1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal");
}

# find the top-level subdirectory and other info based on dataset
if ($asterinfo{$tainteddataset})
{
    $subdir = $asterinfo{$tainteddataset}{"subdir"};
    $label = $asterinfo{$tainteddataset}{"label"};
    $key = $asterinfo{$tainteddataset}{"key"};
    $anchor = $asterinfo{$tainteddataset}{"anchor"};
    $has_aster_granule_id = $asterinfo{$tainteddataset}{"has_aster_granule_id"};
}
else
{
    handleerror("Dataset $tainteddataset is not legal"); # exits
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

# build the directory name for this sensor/path/row/year, using the subdir
my $dirname = $subdir . "/p" . $path . "/r" . $row;

# Variables to hold values for the FGDC page
my $acqDate;
my $LLLong; 
my $URLong;
my $ULLat;
my $LRLat;
my ($band1,$band2,$band3N,$band3B,$band4,$band5,$band6,$band7,$band8,
    $band9,$band10,$band11,$band12,$band13,$band14);

my $metafile = "";
my $name = $sceneid.".meta";
File::Find::find({wanted => sub {
        if (/^$name$/)
        {
            $metafile = $File::Find::name;
        }
    }, untaint => 1},$dirname);


# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;
// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 820;
    winWidth = 390;

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
function loadastermeta(anchor) {
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
        win = open("", "glovismetadataasterglossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes,width=" + winWidth + ",height=" + winHeight);
    }
    else
    {
        win = open("", "glovismetadataasterglossary", "directories=yes,toolbar=yes,menubar=yes,scrollbars=yes,resizable=yes,location=yes,status=yes");
    }
    win.location = "glossary_aster.html#" + anchor;
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
#addFgdcMetadata();
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
    return " " if (!defined($dd) || $dd =~ /^\s*$/);

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
    return " " if (!defined($dd) || $dd =~ /^\s*$/);

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

# subroutine to handle values that might not be set.  If the value is 
# not defined, an empty string is returned.
sub validate_data
{
    my $value = $_[0];

    return " " if (!defined $value || $value =~ /^\s*$/);
    return $value;
}

# read the metadata from the file, format it nicely and according to
# the sensor, and print the html table
sub buildMetaTable
{
    my ($filename,$path,$row) = @_;

    my %metaInfo;   # metadata as read from the file
    my %metadata;   # This is a hash of arrays. Index 0 holds the metadata
                    # value; index 1 holds the anchor name for the glossary

    # This is the order we want the items to appear in the table
    my @order = ();
    push(@order, $label);
    if ($has_aster_granule_id)
    {
        push(@order, 'ASTER Granule ID');
        push(@order, 'ASTER Processing Center');
    }
    push(@order,
                "Acquisition Date",
                "WRS-2 Path",
                "WRS-2 Row",
                "Upper Left Corner",
                "Upper Right Corner",
                "Lower Left Corner",
                "Lower Right Corner",
                "Scene Center",
                "Scene Cloud Cover",
                "SWIR Mode",
                "TIR Mode",
                "VNIR1 Mode",
                "VNIR2 Mode",
                "Day or Night",
                "Orbital Direction",
                "Sun Elevation",
                "Sun Azimuth",
                "Acquisition Time",
                "VNIR Pointing Angle",
                "TIR Pointing Angle",
                "SWIR Pointing Angle"                
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
 
    # get values needed to pass to the FGDC cgi script
    $acqDate = substr($metaInfo{AcqDate},5,2)."-".
               substr($metaInfo{AcqDate},8,2)."-".
               substr($metaInfo{AcqDate},0,4);
    $LLLong = $metaInfo{LLLong};
    $URLong = $metaInfo{URLong};
    $ULLat = $metaInfo{ULLat};
    $LRLat = $metaInfo{LRLat};
    
    # Tempurare
    if($metaInfo{band1_avail})
    {
        $band1 = $metaInfo{band1_avail};
        $band2 = $metaInfo{band2_avail};
        $band3N = $metaInfo{band3N_avail};
        $band3B = $metaInfo{band3B_avail};
        $band4 = $metaInfo{band4_avail};
        $band5 = $metaInfo{band5_avail};
        $band6 = $metaInfo{band6_avail};
        $band7 = $metaInfo{band7_avail};
        $band8 = $metaInfo{band8_avail};
        $band9 = $metaInfo{band9_avail};
        $band10 = $metaInfo{band10_avail};
        $band11 = $metaInfo{band11_avail};
        $band12 = $metaInfo{band12_avail};
        $band13 = $metaInfo{band13_avail};
        $band14 = $metaInfo{band14_avail};
    }
    else
    {
        $band1 = "U";
        $band2 = "U";
        $band3N = "U";
        $band3B = "U";
        $band4 = "U";
        $band5 = "U";
        $band6 = "U";
        $band7 = "U";
        $band8 = "U";
        $band9 = "U";
        $band10 = "U";
        $band11 = "U";
        $band12 = "U";
        $band13 = "U";
        $band14 = "U";
    }

    # add the path and row to the metaInfo hash
    $metaInfo{path} = $path;
    $metaInfo{row} = $row;

    # assign the values to the metadata hash that is used to populate the table

    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    foreach (@order)
    {
        $metadata{$_}[0] = " ";
        $metadata{$_}[1] = "";
    }

    $metadata{$label}[0] = $metaInfo{$key};
    $metadata{$label}[1] = $anchor;

    if ($has_aster_granule_id)
    {
        $metadata{"ASTER Granule ID"}[0]
            = validate_data($metaInfo{AsterGranuleId});
        $metadata{"ASTER Granule ID"}[1] = "aster_granule_id";
        $metadata{"ASTER Processing Center"}[0]
            = validate_data($metaInfo{AsterProcessingCenter});
        $metadata{"ASTER Processing Center"}[1] = "aster_processing_center";
    }

    # include the correct pointing value if one is present
    $metadata{"VNIR Pointing Angle"}[0] = validate_data($metaInfo{VNIRPangle});
    $metadata{"VNIR Pointing Angle"}[1] = "pangle";
    $metadata{"TIR Pointing Angle"}[0] = validate_data($metaInfo{TIRPangle});
    $metadata{"TIR Pointing Angle"}[1] = "pangle";
    $metadata{"SWIR Pointing Angle"}[0] = validate_data($metaInfo{SWIRPangle});
    $metadata{"SWIR Pointing Angle"}[1] = "pangle";
   
    # format the date value
    substr($metaInfo{AcqDate},4,1,"/");
    substr($metaInfo{AcqDate},7,1,"/");
    $metadata{"Acquisition Date"}[0] = $metaInfo{AcqDate};
    $metadata{"Acquisition Date"}[1] = "acquisition_date";
    $metadata{"Acquisition Time"}[0] = $metaInfo{AcqTime};
    $metadata{"Acquisition Time"}[1] = "acq_time";
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
    $metadata{"Day or Night"}[0] = $metaInfo{DayNight};
    $metadata{"Day or Night"}[1] = "day_night";
    $metadata{"Orbital Direction"}[0] = $metaInfo{OrbitDir};
    $metadata{"Orbital Direction"}[1] = "orbit_dir";
    $metadata{"Sun Azimuth"}[0] = $metaInfo{SunAzi};
    $metadata{"Sun Azimuth"}[1] = "sun_azimuth";
    $metadata{"Sun Elevation"}[0] = $metaInfo{SunElev};
    $metadata{"Sun Elevation"}[1] = "sun_elevation";
    # format cloud cover value
    $metaInfo{SceneCc} .= "%";
    $metadata{"Scene Cloud Cover"}[0] = $metaInfo{SceneCc};
    $metadata{"Scene Cloud Cover"}[1] = "cloud_cover";
    $metadata{"SWIR Mode"}[0] = $metaInfo{SWIRMode};
    $metadata{"SWIR Mode"}[1] = "swir_mode";
    $metadata{"TIR Mode"}[0] = $metaInfo{TIRMode};
    $metadata{"TIR Mode"}[1] = "tir_mode";
    $metadata{"VNIR1 Mode"}[0] = $metaInfo{VNIR1Mode};
    $metadata{"VNIR1 Mode"}[1] = "vnir1_mode";
    $metadata{"VNIR2 Mode"}[0] = $metaInfo{VNIR2Mode};
    $metadata{"VNIR2 Mode"}[1] = "vnir2_mode";
    # format the coordinate values
    $tmp = format_DMS_lat($metaInfo{SCLat});
    $metaInfo{SCLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{SCLong});
    $metaInfo{SCLong} = $tmp;
    $metadata{"Scene Center"}[0] = $metaInfo{SCLat}.", ".$metaInfo{SCLong};
    $metadata{"Scene Center"}[1] = "scene_center_lat";
    $metadata{"WRS-2 Path"}[0] = $metaInfo{path};
    $metadata{"WRS-2 Path"}[1] = "wrs_path";
    $metadata{"WRS-2 Row"}[0] = $metaInfo{row};
    $metadata{"WRS-2 Row"}[1] = "wrs_row";

    # generate the table for the metadata
    print "<TABLE BORDER=\"1\" CELLPADDING=\"2\">\n";
    print Tr({-align=>"CENTER",-bgcolor=>"#006666"},
             td(font({-color=>"#FFFFCC"},"Dataset Attribute")),
             td(font({-color=>"#FFFFCC"},"Attribute Value")));
    foreach my $metaItem (@order)
    {
        # if a $metaInfo{} key did not exist, $metadata{} value may be undef
        $metadata{$metaItem} = " " if (!defined($metadata{$metaItem})
                                            || $metadata{$metaItem} =~ /^\s*$/);

        print "<tr><td><a href=\"#\" ";
        print "onClick=\"loadastermeta('$metadata{$metaItem}[1]');\">";
        print "$metaItem</a></td>";
        print "<td>$metadata{$metaItem}[0]</td></tr>\n";
    }
    print "\n</TABLE>\n";
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
    my $dataset;

    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('AsterFGDC.cgi?&scene_id=$sceneid&path=$path&row=$row&bands=$band1\_$band2\_$band3N\_$band3B\_$band4\_$band5\_$band6\_$band7\_$band8\_$band9\_$band10\_$band11\_$band12\_$band13\_$band14&acq_date=$acqDate&LLLong=$LLLong&URLong=$URLong&ULLat=$ULLat&LRLat=$LRLat');\">";
    print "FGDC Metadata</a></p>";
}

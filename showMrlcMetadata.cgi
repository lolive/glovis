#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

use strict;
use CGI qw(:standard escapeHTML);

my %mrlcDir = ("MRLC2K_ARCH"=>"mrlc_2001_tc",  # Terrain Corrected (Radiance)
               "MRLC2K_SITC"=>"mrlc_2001_ra"   # Reflectance Adjusted
              );

# used for date manipulation
my %months_to_numbers = (
        "01" => "JAN",
        "02" => "FEB",
        "03" => "MAR",
        "04" => "APR",
        "05" => "MAY",
        "06" => "JUN",
        "07" => "JUL",
        "08" => "AUG",
        "09" => "SEP",
        "10" => "OCT",
        "11" => "NOV",
        "12" => "DEC",
);

# get the scene id and dataset passed in
my $tainted_sceneid = param('scene_id');
my $tainted_dataset = param('dataset');
my $sensorpath;
my $dataset;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# untaint the scene id
if ($tainted_sceneid !~ m/^[A-Z0-9]{21}$/)
{
    handleerror("Bad scene id $tainted_sceneid");
}

# Because of the two different MRLC database tables for MRLC 2001 terrain
# corrected and 2001 reflectance adjusted there are two different directories
# that the data is stored. MRLC 2001 Terrain Corrected (mrlc_2001_tc, and
# MRLC 2001 Reflectance (mrlc_2001_ra).
if (($tainted_dataset =~ /^(\w{11})$/) and $mrlcDir{$tainted_dataset})
{
    $sensorpath = $mrlcDir{$tainted_dataset};
    $dataset = $1;
}
else
{
    # not a recognized dataset
    handleerror("Unrecognized dataset $tainted_dataset");
}
my $id_offset = 2;

# Variables to hold values for the FGDC page
my $acqDate;
my $LLLong; 
my $URLong;
my $ULLat;
my $LRLat;

# extract the components needed from the scene id to build the path to the 
# actual image
my $path = 0;
my $row = 0;
my $year = 0;
my $sceneid = '';
if ($tainted_sceneid =~ /^(L[ET]\d{14}[A-Z]{3}\d{2})$/) # just like a Landsat ID
{
    $sceneid = $1;
    $path = substr($sceneid,3,3);
    $row  = substr($sceneid,6,3);
    $year = substr($sceneid,9,4);
}
elsif ($tainted_sceneid =~ /^([A-Z]{3}\w\d{17})$/) # or an MRLC ID
{
    $sceneid = $1;
    $path = substr($sceneid,5,3);
    $row  = substr($sceneid,8,3);
    $year = substr($sceneid,15,4);
}
else
{
    handleerror("Can't determine path, row, year from $tainted_sceneid");
}

# build the directory name for this sensor/path/row/year
my $dirname = $sensorpath . "/p" . $path . "/r" . $row . "/y" . $year;
my $image = "";

my $metafile="";

# Build the .meta file name
$metafile = $dirname . "/" . $sceneid . ".meta";

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;

// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 890;
    winWidth = 400;

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
buildMetaTable($metafile);
addFgdcMetadata();
print end_html();
exit;

# routine to convert yes = Y No = N flags into a full word description
sub Yes_No
{
    $_ = $_[0];
    return "Yes" if (m/^[Y|y]/);
    return "No"if (m/^[N|n]/);
    return $_;
}

# routine to convert NULL flags into blank line
sub Filter_Null
{
    $_ = $_[0];
    return "  " if (m/^[NULL|null]/);
    return $_;
}

# routine to convert Sensor flags into a full word description
sub Satellite_Number
{
    $_ = $_[0];
    return "Landsat 4" if (m/^4/);
    return "Landsat 5" if (m/^5/);
    return "Landsat 7" if (m/^7/);
    return $_;
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

# read the metadata from the file, format it nicely and according to
# the sensor, and print the html table
sub buildMetaTable
{
    my ($filename) = @_;
    my @order;
    my %glossaryLinks; # web page links;
    my %metaInfo;      # metadata as read from the file
    my %metadata;      # metadata formatted for display
                       # where keys match values in the @order array

    # Landsat 4,5,7 MRLC Terrain Corrected 2001 Metadata layout
    if ($dataset eq "MRLC2K_ARCH")
    {
        # This is the order we want the items to appear in L4/5,7(MRLC2001)
        # table
        @order = ("Entity ID",
                  "Satellite Number",
                  "Dataset Type",
                  "WRS-2 Path",
                  "WRS-2 Row", 
                  "Acquisition Date",
                  "Date Entered", 
                  "Date Updated",
                  "Map Projection",
                  "Horiz Datum",
                  "Resampling",
                  "Pixel Units",
                  "USGS Projection Number",
                  "Ellipsoid Semi Major",
                  "Ellipsoid Semi Minor", 
                  "Sun Elevation",
                  "Sun Azimuth",
                  "Upper Left Corner",
                  "Upper Right Corner",
                  "Lower Left Corner",
                  "Lower Right Corner",
                  "Proj Parameter1",
                  "Proj Parameter2",
                  "Proj Parameter3",
                  "Proj Parameter4",
                  "Proj Parameter5",
                  "Proj Parameter6",
                  "Proj Parameter7",
                  "Proj Parameter8",
                  "Proj Parameter9",
                  "Proj Parameter10",
                  "Proj Parameter11",
                  "Proj Parameter12",
                  "Proj Parameter13",
                  "Proj Parameter14",
                  "Proj Parameter15",
                  "Browse Avail"
                 );
        # This is a hash holding the links to the web page 
        %glossaryLinks = ("Entity ID" => "entity_id",
                          "Satellite Number" => "satellite",
                          "Dataset Type" => "dataset_type",
                          "WRS-2 Path" => "wrs_path",
                          "WRS-2 Row" => "wrs_row",
                          "Dataset Type" => "dataset_type",
                          "Acquisition Date" => "acquisition_date",
                          "Date Entered" => "date_entered", 
                          "Date Updated" => "date_updated",
                          "Map Projection" => "map_projection_name",
                          "Horiz Datum" => "horiz_datum",
                          "Resampling" => "resampling",
                          "Pixel Units" => "pixel_spacing_units",
                          "USGS Projection Number" => "projection_number",
                          "Ellipsoid Semi Major" => "ellipsoid_semi_major",
                          "Ellipsoid Semi Minor" => "ellipsoid_semi_minor", 
                          "Sun Elevation" => "sun_elevation",
                          "Sun Azimuth" => "sun_azimuth",
                          "Upper Left Corner" => "ul_corner_lat",
                          "Upper Right Corner" => "ur_corner_lat",
                          "Lower Left Corner" => "ll_corner_lat",
                          "Lower Right Corner" => "lr_corner_lat",
                          "Proj Parameter1" => "proj_param", 
                          "Proj Parameter2" => "proj_param", 
                          "Proj Parameter3" => "proj_param", 
                          "Proj Parameter4" => "proj_param", 
                          "Proj Parameter5" => "proj_param", 
                          "Proj Parameter6" => "proj_param", 
                          "Proj Parameter7" => "proj_param", 
                          "Proj Parameter8" => "proj_param", 
                          "Proj Parameter9" => "proj_param", 
                          "Proj Parameter10" => "proj_param", 
                          "Proj Parameter11" => "proj_param", 
                          "Proj Parameter12" => "proj_param", 
                          "Proj Parameter13" => "proj_param", 
                          "Proj Parameter14" => "proj_param", 
                          "Proj Parameter15" => "proj_param", 
                          "Browse Avail" => "browse_avail"
                          );
    }
    # Landsat 4,5,7 MRLC Reflectance Adjusted 2001 Metadata layout
    elsif ($dataset eq "MRLC2K_SITC")
    {
        #This is the order we want the items to appear in L4/5,7(MRLC2001) table
        @order = ("Entity ID", 
                  "Satellite Number",
                  "WRS-2 Path",
                  "WRS-2 Row",
                  "Acquisition Date",
                  "Date Entered",
                  "Date Updated",
                  "Map Projection",
                  "Sun Elevation",
                  "Sun Distance",
                  "B1Gain",
                  "B1Bias", 
                  "B2Gain", 
                  "B2Bias", 
                  "B3Gain", 
                  "B3Bias", 
                  "B4Gain", 
                  "B4Bias", 
                  "B5Gain",
                  "B5Bias",
                  "B6Gain", 
                  "B6Bias", 
                  "Upper Left Corner",
                  "Upper Right Corner",
                  "Lower Left Corner",
                  "Lower Right Corner",
                  "Browse Avail"
                 );
        # This is a hash holding the links to the web page        
        %glossaryLinks = ("Entity ID" => "entity_id", 
                          "Satellite Number" => "satellite",
                          "WRS-2 Path" => "wrs_path",
                          "WRS-2 Row" => "wrs_row",
                          "Acquisition Date" => "acquisition_date",
                          "Date Entered" => "date_entered",
                          "Date Updated" => "date_modified",
                          "Map Projection" => "map_projection",
                          "Sun Elevation" => "sun_elevation",
                          "Sun Distance" => "sun_distance",
                          "B1Gain" => "band_gain",
                          "B1Bias" => "band_bias", 
                          "B2Gain" => "band_gain", 
                          "B2Bias" => "band_bias", 
                          "B3Gain" => "band_gain", 
                          "B3Bias" => "band_bias", 
                          "B4Gain" => "band_gain", 
                          "B4Bias" => "band_bias", 
                          "B5Gain" => "band_gain",
                          "B5Bias" => "band_bias",
                          "B6Gain" => "band_gain", 
                          "B6Bias" => "band_bias", 
                          "Upper Left Corner" => "ul_corner_lat",
                          "Upper Right Corner" => "ur_corner_lat",
                          "Lower Left Corner" => "ll_corner_lat",
                          "Lower Right Corner" => "lr_corner_lat",
                          "Browse Avail" => "browse_avail"
                          );
    }

    # read the lines from the metadata file
    open (META,"<".$filename) or handleerror("Cannot open $filename: $!");
    
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
    $acqDate = substr($metaInfo{AcqDate},4,2)."-".
               substr($metaInfo{AcqDate},6,2)."-".
               substr($metaInfo{AcqDate},0,4);

    $LLLong = $metaInfo{LLLong};
    $URLong = $metaInfo{URLong};
    $ULLat = $metaInfo{ULLat};
    $LRLat = $metaInfo{LRLat};

    # assign the values to the metadata hash that is used to populate the table

    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    @metadata{@order} = ("") x scalar(@order);

    $metadata{"WRS-2 Path"} = $metaInfo{path};
    $metadata{"WRS-2 Row"} = $metaInfo{row};
    $metadata{"Satellite Number"} = Satellite_Number($metaInfo{sensor});
    $metadata{"Date Entered"} = $metaInfo{DateEntered};
    
    #format the Acquisition Date value
    my $date = substr($metaInfo{AcqDate},6,2) . "-" .
            $months_to_numbers{substr($metaInfo{AcqDate},4,2)} . "-".
            substr($metaInfo{AcqDate},2,2);
    $metaInfo{AcqDate} = $date; 
    $metadata{"Acquisition Date"} = $metaInfo{AcqDate};
    
    # Separating the fields that are needed by sensor to avoid an error
    # of uninitialized value.
    # Landsat 4,5,7 MRLC Terrain Corrected 2001 Metadata layout
    if ($dataset eq "MRLC2K_ARCH")
    {
        $metadata{"Date Updated"} = Filter_Null($metaInfo{DateUpdated});
        $metadata{"Map Projection"} = Filter_Null($metaInfo{MapProjName});
        $metadata{"Pixel Units"} = Filter_Null($metaInfo{PixelSpacingUnits});
        $metadata{"Entity ID"} = $metaInfo{entityID};
        $metadata{"Dataset Type"} = Filter_Null($metaInfo{DatasetType});
        $metadata{"USGS Projection Number"} = Filter_Null($metaInfo{ProjNmbr});
        $metadata{"Horiz Datum"} = Filter_Null($metaInfo{HorizDatum});
        $metadata{"Ellipsoid Semi Major"} = Filter_Null($metaInfo{EllipsoidMajor});
        $metadata{"Ellipsoid Semi Minor"} = Filter_Null($metaInfo{EllipsoidMinor});
        $metadata{"Resampling"} = Filter_Null($metaInfo{Resampling});
        $metadata{"Sun Elevation"} = Filter_Null($metaInfo{SunElev});
        $metadata{"Sun Azimuth"} = Filter_Null($metaInfo{SunAzi});
        $metadata{"Proj Parameter1"} = Filter_Null($metaInfo{ProjParam1});
        $metadata{"Proj Parameter2"} = Filter_Null($metaInfo{ProjParam2});
        $metadata{"Proj Parameter3"} = Filter_Null($metaInfo{ProjParam3});
        $metadata{"Proj Parameter4"} = Filter_Null($metaInfo{ProjParam4});
        $metadata{"Proj Parameter5"} = Filter_Null($metaInfo{ProjParam5});
        $metadata{"Proj Parameter6"} = Filter_Null($metaInfo{ProjParam6});
        $metadata{"Proj Parameter7"} = Filter_Null($metaInfo{ProjParam7});
        $metadata{"Proj Parameter8"} = Filter_Null($metaInfo{ProjParam8});
        $metadata{"Proj Parameter9"} = Filter_Null($metaInfo{ProjParam9});
        $metadata{"Proj Parameter10"} = Filter_Null($metaInfo{ProjParam10});
        $metadata{"Proj Parameter11"} = Filter_Null($metaInfo{ProjParam11});
        $metadata{"Proj Parameter12"} = Filter_Null($metaInfo{ProjParam12});
        $metadata{"Proj Parameter13"} = Filter_Null($metaInfo{ProjParam13});
        $metadata{"Proj Parameter14"} = Filter_Null($metaInfo{ProjParam14});
        $metadata{"Proj Parameter15"} = Filter_Null($metaInfo{ProjParam15});
        $metadata{"Browse Avail"} = Yes_No($metaInfo{BrowseAvail});
    }
    # Landsat 4,5,7 MRLC Reflectance Adjusted 2001 Metadata layout
    elsif ($dataset eq "MRLC2K_SITC")
    {
        $metadata{"Date Updated"} = Filter_Null($metaInfo{DateUpdated});
        $metadata{"Map Projection"} = Filter_Null($metaInfo{MapProj});
        $metadata{"Sun Elevation"} = Filter_Null($metaInfo{SunElev});
        $metadata{"Entity ID"} = $metaInfo{entityID};
        $metadata{"Sun Distance"} = Filter_Null($metaInfo{SunAzi});
        $metadata{"B1Gain"} = Filter_Null($metaInfo{B1Gain});
        $metadata{"B1Bias"} = Filter_Null($metaInfo{B1Bias});
        $metadata{"B2Gain"} = Filter_Null($metaInfo{B2Gain});
        $metadata{"B2Bias"} = Filter_Null($metaInfo{B2Bias});
        $metadata{"B3Gain"} = Filter_Null($metaInfo{B3Gain});
        $metadata{"B3Bias"} = Filter_Null($metaInfo{B3Bias});
        $metadata{"B4Gain"} = Filter_Null($metaInfo{B4Gain});
        $metadata{"B4Bias"} = Filter_Null($metaInfo{B4Bias});
        $metadata{"B5Gain"} = Filter_Null($metaInfo{B5Gain});
        $metadata{"B5Bias"} = Filter_Null($metaInfo{B5Bias});
        $metadata{"B6Gain"} = Filter_Null($metaInfo{B6Gain});
        $metadata{"B6Bias"} = Filter_Null($metaInfo{B6Bias});
        $metadata{"Browse Avail"} = Yes_No($metaInfo{BrowseAvail});
    }
 
    my $tmp;

    #format ULLat coordinate values
    $tmp = format_DMS_lat($metaInfo{ULLat});
    $metaInfo{ULLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{ULLong});
    $metaInfo{ULLong} = $tmp;
    $metadata{"Upper Left Corner"} = $metaInfo{ULLat}.", ".$metaInfo{ULLong};

    #format URLat coordinate values
    $tmp = format_DMS_lat($metaInfo{URLat});
    $metaInfo{URLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{URLong});
    $metaInfo{URLong} = $tmp;
    $metadata{"Upper Right Corner"} = $metaInfo{URLat}.", ".$metaInfo{URLong};

    #format LRLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LRLat});
    $metaInfo{LRLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LRLong});
    $metaInfo{LRLong} = $tmp;
    $metadata{"Lower Right Corner"} = $metaInfo{LRLat}.", ".$metaInfo{LRLong};

    #format LLLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LLLat});
    $metaInfo{LLLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LLLong});
    $metaInfo{LLLong} = $tmp;
    $metadata{"Lower Left Corner"} = $metaInfo{LLLat}.", ".$metaInfo{LLLong};
    
    # Given the different glossary pages for MRLC 2001 TC & MRLC 2001 RA we
    # need the win.location in the Javascript to point to the correct glossary
    # page.
    my $page;
    if ($dataset eq "MRLC2K_ARCH")
    {
        $page = "glossary_mrlc_2001_tc.html#";
    }
    elsif ($dataset eq "MRLC2K_SITC")
    {
        $page = "glossary_mrlc_2001_ra.html#";
    }

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
}

# routine to generate an error page  
sub handleerror
{
    my @messages = @_; # mainly for debugging - don't print details in prod

    print header();
    print start_html("USGS Global Visualization Viewer - Error");
    print p("Error generating metadata page");
    # Un-comment this for debugging
    #foreach(@messages)
    #{
    #    print p($_);
    #}
    print end_html();
    exit;
}
# routine to add link to FGDC metadata
sub addFgdcMetadata
{
    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('MrlcFGDC.cgi?scene_id=$sceneid&dataset=$dataset&acq_date=$acqDate&LLLong=$LLLong&URLong=$URLong&ULLat=$ULLat&LRLat=$LRLat');\">";
    print "FGDC Metadata</a></p>";
}

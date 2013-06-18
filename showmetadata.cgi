#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

use strict;
use locale;
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $tainted_sceneid = param('scene_id');

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# untaint the scene id
unless($tainted_sceneid =~ m#^([\w.-]+)$#)
{
    handleerror();
}
my $sceneid = $1;

# make sure it is really a Landsat scene id
if ($sceneid !~ /^L[OCEMT]\d{14}\w{3}\d{2}$/)
{
    handleerror("Scene ID $sceneid is not legal"); # exits
}
# Note: O = OLI (L8), C = Combined OLI/TIRS (L8), E = ETM (L7), M = MSS (L4-5)
# T = TM if sensor is 4 or 5, could be TIRS if 8 but GloVis doesn't use thermal

# extract the components needed from the scene id to build the path to the 
# actual image
my $mode = substr($sceneid,1,1);
my $sensor = substr($sceneid,2,1);
my $path = substr($sceneid,3,3);
my $row = substr($sceneid,6,3);
my $year = substr($sceneid,9,4);

# make a version of the sensor for the path to the image since L4 and 5 are
# both under the l5 directory
my $sensorpath = $sensor;
$sensorpath .= "oli" if ($sensor == 8);
$sensorpath = "5" if ($sensor == 4);

# Because the TM and MSS sensor are aboard Landsat 4 and 5 satellites we have
# to use the sensor number and the instrument mode to find the correct
# directory path for the right sensor type: (4/5 TM = /l5),(4/5 MSS = /l4_5mss),
# & (ETM = /l7)
if (($sensorpath == 5) && ($mode eq "M"))
{
    $sensorpath = "4_5mss";
}
elsif ($sensor <= 3)
{
    # Landsat 1-3 are in /l1_3mss
    $sensorpath = "1_3mss";
}

# build the directory name for this sensor/path/row/year
my $dirname = "l" . $sensorpath . "/p" . $path . "/r" . $row . "/y" . $year;
my $image = "";

my $metafile="";

# Build the .meta file name
$metafile = $dirname . "/" . $sceneid . ".meta";

# If the file doesn't exist and the sensor is L7, try the slc-off data set
my $is_slc_off = 0;
if (($sensor == 7) && ($year >= 2003) && (!-e $metafile))
{
    my $slc_off_dirname = "l7slc_off/p" . $path . "/r" . $row . "/y" . $year;
    my $alternate_metafile = $slc_off_dirname . "/" . $sceneid . ".meta";
    if (-e $alternate_metafile)
    {
        $metafile = $alternate_metafile;
        $dirname = $slc_off_dirname;
        $is_slc_off = 1;
    }
}

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;

// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 890;
    winWidth = 480;

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

# routine to format the cloud cover value
sub format_cloud_cover
{
    my $cc = $_[0];
    if (defined $cc && $cc !~ /^\s*$/)
    {
        if (($cc > 0.0) && ($cc < 1.0))
        {
            $cc = "0" . $cc if ($cc =~ /^\./);
        }
        $cc .= "%";
    }
    else
    {
        $cc = "";
    }

    return $cc;
}

# routine to convert value of 99 into a null value represented by a blank line
sub Filter_SunElevation
{
    $_ = $_[0];
    return "" if (m/^99/);
    return $_;
}
    
# routines to format a decimal degree value as a DMS value and tack it on to 
# the decimal degrees value
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

    my $original = $dd;

    # set the N/S flag
    my $direction = "N";
    if ($dd < 0)
    {
        $direction = "S";
        $dd = -$dd;
    }

    # convert to a DMS string
    my $dms = format_DMS($dd,$direction);
    return "$original ($dms)";
}

# routine to format DMS values for Longitudes
sub format_DMS_lon
{
    my $dd = $_[0]; # get the decimal degrees
    return "" if (!defined($dd) || $dd =~ /^\s*$/);

    my $original = $dd;
    # set the E/W flag
    my $direction = "E";
    if ($dd < 0)
    {
        $direction = "W";
        $dd = -$dd;
    }
    # convert to a DMS string
    my $dms = format_DMS($dd,$direction);
    return "$original ($dms)";
}

# routine to format the Data Type Level 1 display
sub format_data_type_level_1
{
    my $data_type_level_1 = $_[0]; # get the data type

    # if glovis doesn't know the Level 1 product, assume processing is required
    return "Processing Required" if (!defined($data_type_level_1));

    # if the Level 1 product data type is blank, processing is required
    return "Processing Required" if ($data_type_level_1 =~ /^(\s*|NULL|PR)$/i);

    return $data_type_level_1; # as is
}

# read the metadata from the file, format it nicely and according to
# the sensor, and print the html table
sub buildMetaTable
{
    my ($filename) = @_;
    my @order;         # attributes to display (depends on sensor)
                       # these also serve as hash keys
    my @l1order;       # attributes to display iff L1 exists
    my %glossaryLinks; # web page links (name of the anchor)
    my %metaInfo;      # metadata as read from the file
    my %metadata;      # metadata formatted for display
                       # whose keys match values in the @order array
    my $display_level_1 = 0; # should we display the @l1order fields?

    # Landsat 4/5 TM Metadata layout
    if ((($sensor == 4) || ($sensor == 5)) && ($mode eq "T"))
    {
        #This is the order we want the items to appear in the L5(TM) table
        @order = ("Landsat Scene Identifier",
                  "Spacecraft Identifier",
                  "Sensor Mode",
                  "Station Identifier", 
                  "Day Night",
                  "WRS Path",
                  "WRS Row",
                  "Date Acquired",
                  "Start Time",
                  "Stop Time", 
                  "Sensor Anomalies", 
                  "Acquisition Quality",
                  "Quality Band 1",
                  "Quality Band 2",
                  "Quality Band 3",
                  "Quality Band 4",
                  "Quality Band 5",
                  "Quality Band 6",
                  "Quality Band 7",
                  "Cloud Cover",
                  "Cloud Cover Quadrant Upper Left",
                  "Cloud Cover Quadrant Upper Right",
                  "Cloud Cover Quadrant Lower Left",
                  "Cloud Cover Quadrant Lower Right",
                  "Sun Elevation",
                  "Sun Azimuth",
                  "Scene Center Latitude",
                  "Scene Center Longitude",
                  "Corner Upper Left Latitude",
                  "Corner Upper Left Longitude",
                  "Corner Upper Right Latitude",
                  "Corner Upper Right Longitude",
                  "Corner Lower Left Latitude",
                  "Corner Lower Left Longitude",
                  "Corner Lower Right Latitude",
                  "Corner Lower Right Longitude",
                  "Browse Exists",
                  "Scene Mode",
                  "Data Category",
                  "Map Projection L0Ra",
                  "Data Type L0Rp",
                  "Data Type Level 1" # This is the key to displaying @l1order
                 );
        @l1order = (
            "Elevation Source",
            "Output Format",
            "Ephemeris Type",
            "Corner Upper Left Latitude Product",
            "Corner Upper Left Longitude Product",
            "Corner Upper Right Latitude Product",
            "Corner Upper Right Longitude Product",
            "Corner Lower Left Latitude Product",
            "Corner Lower Left Longitude Product",
            "Corner Lower Right Latitude Product",
            "Corner Lower Right Longitude Product",
            "Reflective Lines",
            "Reflective Samples",
            "Thermal Lines",
            "Thermal Samples",
            "Ground Control Points Model",
            "Geometric RMSE Model",
            "Geometric RMSE Model X",
            "Geometric RMSE Model Y",
            "Ground Control Points Verify",
            "Geometric RMSE Verify",
            "Map Projection L1",
            "Datum",
            "Ellipsoid",
            "UTM Zone",
            "Vertical Longitude from Pole",
            "True Scale Latitude",
            "False Easting",
            "False Northing",
            "Grid Cell Size Reflective",
            "Grid Cell Size Thermal",
            "Orientation",
            "Resampling Option"
        );
     }
     # Landsat 8 OLI Metadata layout
     elsif ($sensor == 8)
     {
        # This is the order we want the items to appear in the L8 OLI table
        @order = ("Landsat Scene Identifier",
                  "Station Identifier", 
                  "Day Night",
                  "WRS Path",
                  "WRS Row",
                  "Date Acquired",
                  "Start Time",
                  "Stop Time",
                  "Image Quality",
                  "Cloud Cover",
                  "Sun Elevation",
                  "Sun Azimuth",
                  "Scene Center Latitude",
                  "Scene Center Longitude",
                  "Corner Upper Left Latitude",
                  "Corner Upper Left Longitude",
                  "Corner Upper Right Latitude",
                  "Corner Upper Right Longitude",
                  "Corner Lower Left Latitude",
                  "Corner Lower Left Longitude",
                  "Corner Lower Right Latitude",
                  "Corner Lower Right Longitude",
                  "Browse Exists",
                  "Data Category",
                  "Data Type L0Rp",
                  "Data Type Level 1");
        # L8 in GloVis is always level 1, so these will always get shown:
        push(@l1order,
            "Geometric RMSE Model X",
            "Geometric RMSE Model Y",
        );
     }
     # Landsat 7 ETM (SLC-on and SLC-off) Metadata layout
     elsif ($sensor == 7)
     {
        # This is the order we want the items to appear in the L7(ETM) table
        @order = ("Landsat Scene Identifier",
                  "Sensor Mode",
                  "Station Identifier", 
                  "Day Night",
                  "WRS Path",
                  "WRS Row",
                  "Date Acquired",
                  "Start Time",
                  "Stop Time",
                  "Image Quality VCID 1",
                  "Image Quality VCID 2",
                  "Cloud Cover",
                  "Cloud Cover Quadrant Upper Left",
                  "Cloud Cover Quadrant Upper Right",
                  "Cloud Cover Quadrant Lower Left",
                  "Cloud Cover Quadrant Lower Right",
                  "Sun Elevation",
                  "Sun Azimuth",
                  "Scene Center Latitude",
                  "Scene Center Longitude",
                  "Corner Upper Left Latitude",
                  "Corner Upper Left Longitude",
                  "Corner Upper Right Latitude",
                  "Corner Upper Right Longitude",
                  "Corner Lower Left Latitude",
                  "Corner Lower Left Longitude",
                  "Corner Lower Right Latitude",
                  "Corner Lower Right Longitude",
                  "Full Aperture Calibration",
                  "Gain Band 1",
                  "Gain Band 2", 
                  "Gain Band 3",
                  "Gain Band 4", 
                  "Gain Band 5",
                  "Gain Band 6 VCID 1",
                  "Gain Band 6 VCID 2",
                  "Gain Band 7", 
                  "Gain Band 8", 
                  "Gain Change Band 1",
                  "Gain Change Band 2",
                  "Gain Change Band 3",
                  "Gain Change Band 4",
                  "Gain Change Band 5",
                  "Gain Change Band 6 VCID 1",
                  "Gain Change Band 6 VCID 2",
                  "Gain Change Band 7", 
                  "Gain Change Band 8",
                  "Browse Exists",
                  "Data Category",
        );
        if ($is_slc_off)
        {
            push(@order, "Gap Phase Source", "Gap Phase Statistic");
        }
        push(@order, "Data Type Level 1"); # This is the key for L1 fields
        @l1order = (
            "Elevation Source",
            "Output Format",
            "Ephemeris Type",
            "Corner Upper Left Latitude Product",
            "Corner Upper Left Longitude Product",
            "Corner Upper Right Latitude Product",
            "Corner Upper Right Longitude Product",
            "Corner Lower Left Latitude Product",
            "Corner Lower Left Longitude Product",
            "Corner Lower Right Latitude Product",
            "Corner Lower Right Longitude Product",
            "Panchromatic Lines",
            "Panchromatic Samples",
            "Reflective Lines",
            "Reflective Samples",
            "Thermal Lines",
            "Thermal Samples"
        );
        if ($is_slc_off)
        {
            push(@l1order, "Date Acquired Gap Fill", "Gap Fill");
        }
        push(@l1order,
            "Ground Control Points Model",
            "Geometric RMSE Model",
            "Geometric RMSE Model X",
            "Geometric RMSE Model Y",
            "Map Projection L1",
            "Datum",
            "Ellipsoid",
            "UTM Zone",
            "Vertical Longitude from Pole",
            "True Scale Latitude",
            "False Easting",
            "False Northing",
            "Grid Cell Size Panchromatic",
            "Grid Cell Size Reflective",
            "Grid Cell Size Thermal",
            "Orientation",
            "Resampling Option"
        );
        if ($is_slc_off)
        {
            push(@l1order, "Scan Gap Interpolation");
        }
    }
    # Landsat 1-5 MSS Metadata layout
    else
    {
        # set the starting band for the sensor
        my $startBand = 4;
        $startBand = 1 if ($sensor > 3);

        #This is the order we want the items to appear in the L1-5(MSS) table
        @order = ("Landsat Scene Identifier",
                  "Spacecraft Identifier",
                  "Sensor Mode",
                  "Station Identifier",
                  "Day Night",
                  "WRS Path",
                  "WRS Row",
                  "WRS Type",
                  "Date Acquired",
                  "Start Time",
                  "Stop Time",
                  "Sensor Anomalies",
                  "Acquisition Quality",
                  "Quality Band " . $startBand,
                  "Quality Band " . ($startBand + 1),
                  "Quality Band " . ($startBand + 2),
                  "Quality Band " . ($startBand + 3),
                  "Cloud Cover",
                  "Cloud Cover Quadrant Upper Left",
                  "Cloud Cover Quadrant Upper Right",
                  "Cloud Cover Quadrant Lower Left",
                  "Cloud Cover Quadrant Lower Right",
                  "Sun Elevation",
                  "Sun Azimuth",
                  "Scene Center Latitude",
                  "Scene Center Longitude",
                  "Corner Upper Left Latitude",
                  "Corner Upper Left Longitude",
                  "Corner Upper Right Latitude",
                  "Corner Upper Right Longitude",
                  "Corner Lower Left Latitude",
                  "Corner Lower Left Longitude",
                  "Corner Lower Right Latitude",
                  "Corner Lower Right Longitude",
                  "Gain Band " . $startBand,
                  "Gain Band " . ($startBand + 1),
                  "Gain Band " . ($startBand + 2),
                  "Gain Band " . ($startBand + 3),
                  "Browse Exists",
                  "Scene Mode",
                  "Data Category",
                  "Map Projection L0Ra",
                  "Data Type L0Rp",
                  "Data Type Level 1",
                 );
        @l1order = (
            "Elevation Source",
            "Output Format",
            "Ephemeris Type",
            "Corner Upper Left Latitude Product",
            "Corner Upper Left Longitude Product",
            "Corner Upper Right Latitude Product",
            "Corner Upper Right Longitude Product",
            "Corner Lower Left Latitude Product",
            "Corner Lower Left Longitude Product",
            "Corner Lower Right Latitude Product",
            "Corner Lower Right Longitude Product",
            "Reflective Lines",
            "Reflective Samples",
            "Ground Control Points Model",
            "Geometric RMSE Model",
            "Geometric RMSE Model X",
            "Geometric RMSE Model Y",
            "Ground Control Points Verify",
            "Geometric RMSE Verify",
            "Map Projection L1",
            "Datum",
            "Ellipsoid",
            "UTM Zone",
            "Vertical Longitude from Pole",
            "True Scale Latitude",
            "False Easting",
            "False Northing",
            "Grid Cell Size Reflective",
            "Orientation",
            "Resampling Option"
        );
    }

    # This is a hash holding names of the glossary web page anchors
    # Key is how it is displayed prettily on this page as listed in @order
    # or @l1order, value is the anchor in landsat_dictionary.html
    %glossaryLinks = 
         (
          "Acquisition Quality" => "acquisition_quality",
          "Browse Exists" => "browse_exists",
          "Cloud Cover" => "cloud_cover",
          "Cloud Cover Quadrant Upper Left"
                => "cloud_cover_quadrant_upper_left",
          "Cloud Cover Quadrant Upper Right"
                => "cloud_cover_quadrant_upper_right",
          "Cloud Cover Quadrant Lower Left"
                => "cloud_cover_quadrant_lower_left",
          "Cloud Cover Quadrant Lower Right"
                => "cloud_cover_quadrant_lower_right",
          "Corner Upper Left Latitude" => "corner_upper_left_latitude",
          "Corner Upper Left Latitude Product"
            => "corner_upper_left_latitude_product",
          "Corner Upper Left Longitude" => "corner_upper_left_longitude",
          "Corner Upper Left Longitude Product"
            => "corner_upper_left_longitude_product",
          "Corner Upper Right Latitude" => "corner_upper_right_latitude",
          "Corner Upper Right Latitude Product"
            => "corner_upper_right_latitude_product",
          "Corner Upper Right Longitude" => "corner_upper_right_longitude",
          "Corner Upper Right Longitude Product"
            => "corner_upper_right_longitude_product",
          "Corner Lower Left Latitude" => "corner_lower_left_latitude",
          "Corner Lower Left Latitude Product"
            => "corner_lower_left_latitude_product",
          "Corner Lower Left Longitude" => "corner_lower_left_longitude",
          "Corner Lower Left Longitude Product"
            => "corner_lower_left_longitude_product",
          "Corner Lower Right Latitude" => "corner_lower_right_latitude",
          "Corner Lower Right Latitude Product"
            => "corner_lower_right_latitude_product",
          "Corner Lower Right Longitude" => "corner_lower_right_longitude",
          "Corner Lower Right Longitude Product"
            => "corner_lower_right_longitude_product",
          "Data Category" => "data_category",
          "Data Type L0Rp" => "data_type_l0rp",
          "Data Type Level 1" => "data_type_level_1",
          "Date Acquired" => "date_acquired",
          "Date Acquired Gap Fill" => "date_acquired_gap_fill",
          "Datum" => "datum",
          "Day Night" => "day_night",
          "Elevation Source" => "elevation_source",
          "Ellipsoid" => "ellipsoid",
          "Ephemeris Type" => "ephemeris_type",
          "False Easting" => "false_easting",
          "False Northing" => "false_northing",
          "Full Aperture Calibration" => "full_aperture_calibration",
          "Gain Band 1" => "gain_band_1",
          "Gain Band 2" => "gain_band_1", 
          "Gain Band 3" => "gain_band_1",
          "Gain Band 4" => "gain_band_1", 
          "Gain Band 5" => "gain_band_1",
          "Gain Band 6" => "gain_band_1",
          "Gain Band 6 VCID 1" => "gain_band_1",
          "Gain Band 6 VCID 2" => "gain_band_1",
          "Gain Band 7" => "gain_band_1", 
          "Gain Band 8" => "gain_band_1", 
          "Gain Change Band 1" => "gain_change_band_1",
          "Gain Change Band 2" => "gain_change_band_1",
          "Gain Change Band 3" => "gain_change_band_1",
          "Gain Change Band 4" => "gain_change_band_1",
          "Gain Change Band 5" => "gain_change_band_1",
          "Gain Change Band 6" => "gain_change_band_1",
          "Gain Change Band 6 VCID 1" => "gain_change_band_1",
          "Gain Change Band 6 VCID 2" => "gain_change_band_1",
          "Gain Change Band 7" => "gain_change_band_1", 
          "Gain Change Band 8" => "gain_change_band_1",
          "Gap Fill" => "gap_fill",
          "Gap Phase Source" => "gap_phase_source",
          "Gap Phase Statistic" => "gap_phase_statistic",
          "Geometric RMSE Model" => "geometric_rmse_model",
          "Geometric RMSE Model X" => "geometric_rmse_model_x",
          "Geometric RMSE Model Y" => "geometric_rmse_model_y",
          "Geometric RMSE Verify" => "geometric_rmse_verify",
          "Grid Cell Size Panchromatic" => "grid_cell_size_panchromatic",
          "Grid Cell Size Reflective" => "grid_cell_size_reflective",
          "Grid Cell Size Thermal" => "grid_cell_size_thermal",
          "Ground Control Points Model" => "ground_control_points_model",
          "Ground Control Points Verify" => "ground_control_points_verify",
          "Image Quality" => "image_quality_VCID_1",
          "Image Quality VCID 1" => "image_quality_VCID_1",
          "Image Quality VCID 2" => "image_quality_VCID_1",
          "Landsat Scene Identifier" => "landsat_scene_identifier",
          "Map Projection L0Ra" => "map_projection_l0ra",
          "Map Projection L1" => "map_projection_l1",
          "Orientation" => "orientation",
          "Output Format" => "output_format",
          "Panchromatic Lines" => "panchromatic_lines",
          "Panchromatic Samples" => "panchromatic_samples",
          "Quality Band 1" => "quality_band_1",
          "Quality Band 2" => "quality_band_1",
          "Quality Band 3" => "quality_band_1",
          "Quality Band 4" => "quality_band_1",
          "Quality Band 5" => "quality_band_1",
          "Quality Band 6" => "quality_band_1",
          "Quality Band 6 VCID 1" => "quality_band_1",
          "Quality Band 6 VCID 2" => "quality_band_1",
          "Quality Band 7" => "quality_band_1",
          "Quality Band 8" => "quality_band_1",
          "Reflective Lines" => "reflective_lines",
          "Reflective Samples" => "reflective_samples",
          "Resampling Option" => "resampling_option",
          "Scan Gap Interpolation" => "scan_gap_interpolation",
          "Scene Center Latitude" => "scene_center_latitude",
          "Scene Center Longitude" => "scene_center_longitude",
          "Scene Mode" => "scene_mode",
          "Sensor Anomalies" => "sensor_anomalies",
          "Sensor Mode" => "sensor_mode",
          "Spacecraft Identifier" => "spacecraft_identifier",
          "Start Time" => "start_time",
          "Station Identifier" => "station_identifier",
          "Stop Time" => "stop_time",
          "Sun Elevation" => "sun_elevation",
          "Sun Azimuth" => "sun_azimuth",
          "Thermal Lines" => "thermal_lines",
          "Thermal Samples" => "thermal_samples",
          "True Scale Latitude" => "true_scale_latitude",
          "UTM Zone" => "utm_zone",
          "Vertical Longitude from Pole" => "vertical_longitude_from_pole",
          "WRS Path" => "wrs_path",
          "WRS Row"=> "wrs_row",
          "WRS Type"=> "wrs_type",
         );

    # read the lines from the metadata file
    open (META,"<".$filename) or handleerror("Can't open $filename: $!");
    
    while (<META>)
    {
        chomp; 
        next if (/^\s*$/); # Skip any blank lines
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

    # set the WRS type
    $metadata{"WRS Type"} = ($sensor <= 3 ? 1 : 2);

    # set the fields that apply across all Landsat sensors
    $metadata{"Landsat Scene Identifier"} = $metaInfo{entityID};
    $metadata{"WRS Path"} = sprintf("%03d", $metaInfo{path});
    $metadata{"WRS Row"} = sprintf("%03d", $metaInfo{row});

    $metadata{"Spacecraft Identifier"} = $sensor;
    $metadata{"Station Identifier"} = $metaInfo{station_id};
    $metadata{"Sun Azimuth"} = $metaInfo{sun_azimuth};
    $metadata{"Sun Elevation"} = Filter_SunElevation($metaInfo{sun_elevation});
    $metadata{"Day Night"} = $metaInfo{day_night};
    $metadata{"Start Time"} = $metaInfo{start_time};
    $metadata{"Stop Time"} = $metaInfo{stop_time};
    $metadata{"Browse Exists"} = $metaInfo{browse_exists}; 
    $metadata{"Data Category"} = $metaInfo{data_category};

    # Landsat 8 does not have Sensor Mode, at least not in SID_SCENE_USER_DETAIL
    if (exists($metaInfo{sensor_mode}))
    {
        $metadata{"Sensor Mode"} = $metaInfo{sensor_mode};
    }

    # These two fields apply across all sensors but were added August 2012
    # so older metadata will not have them
    if (exists($metaInfo{map_projection_l0ra}))
    {
        $metadata{"Map Projection L0Ra"} = $metaInfo{map_projection_l0ra};
    }
    if (exists($metaInfo{data_type_l0rp}))
    {
        $metadata{"Data Type L0Rp"} = $metaInfo{data_type_l0rp};
    }
     
    # format the Date Acquired value
    my $date = substr($metaInfo{date_acquired},0,4) . "/" .
               substr($metaInfo{date_acquired},4,2) . "/" .
               substr($metaInfo{date_acquired},6,2);
    $metaInfo{date_acquired} = $date;
    $metadata{"Date Acquired"} = $metaInfo{date_acquired};

    # format upper-left coordinate values
    $metadata{"Corner Upper Left Latitude"}
            = format_DMS_lat($metaInfo{corner_ul_lat});
    $metadata{"Corner Upper Left Longitude"}
            = format_DMS_lon($metaInfo{corner_ul_lon});

    # format upper-right coordinate values
    $metadata{"Corner Upper Right Latitude"}
            = format_DMS_lat($metaInfo{corner_ur_lat});
    $metadata{"Corner Upper Right Longitude"}
            = format_DMS_lon($metaInfo{corner_ur_lon});

    # format lower-right coordinate values
    $metadata{"Corner Lower Right Latitude"}
            = format_DMS_lat($metaInfo{corner_lr_lat});
    $metadata{"Corner Lower Right Longitude"}
            = format_DMS_lon($metaInfo{corner_lr_lon});

    # format lower-left coordinate values
    $metadata{"Corner Lower Left Latitude"}
            = format_DMS_lat($metaInfo{corner_ll_lat});
    $metadata{"Corner Lower Left Longitude"}
            = format_DMS_lon($metaInfo{corner_ll_lon});

    # format the Scene Center coordinate values
    $metadata{"Scene Center Latitude"}
            = format_DMS_lat($metaInfo{scene_center_lat});
    $metadata{"Scene Center Longitude"}
            = format_DMS_lon($metaInfo{scene_center_lon});

    # format cloud cover values
    $metadata{"Cloud Cover"} = format_cloud_cover($metaInfo{cloud_cover});
    $metadata{"Cloud Cover Quadrant Upper Left"}
                    = format_cloud_cover($metaInfo{cloud_cover_quad_ul});
    $metadata{"Cloud Cover Quadrant Upper Right"}
                    = format_cloud_cover($metaInfo{cloud_cover_quad_ur});
    $metadata{"Cloud Cover Quadrant Lower Left"}
                    = format_cloud_cover($metaInfo{cloud_cover_quad_ll});
    $metadata{"Cloud Cover Quadrant Lower Right"}
                    = format_cloud_cover($metaInfo{cloud_cover_quad_lr});

    # These fields vary depending on the Landsat sensor, as listed in @order
    # so check first whether a default value was given in %metadata.
    # (Fortunately there is now a lot of consistency across Landsat sensors
    # so a field doesn't have different naming conventions in each sensor!)

    if (exists($metadata{"Acquisition Quality"}))
    {
        $metadata{"Acquisition Quality"} = $metaInfo{acquisition_quality};
    }

    if (exists($metadata{"Image Quality"}))
    {
        $metadata{"Image Quality"} = $metaInfo{image_quality};
    }

    if (exists($metadata{"Image Quality VCID 1"}))
    {
        $metadata{"Image Quality VCID 1"} = $metaInfo{image_quality_vcid_1};
    }

    if (exists($metadata{"Image Quality VCID 2"}))
    {
        $metadata{"Image Quality VCID 2"} = $metaInfo{image_quality_vcid_2};
    }

    if (exists($metadata{"Sensor Anomalies"}))
    {
        $metadata{"Sensor Anomalies"} = $metaInfo{sensor_anomalies};
    }

    if (exists($metadata{"Scene Mode"}))
    {
        $metadata{"Scene Mode"} = $metaInfo{scene_mode};
    }

    if (exists($metadata{"Full Aperture Calibration"}))
    {
        $metadata{"Full Aperture Calibration"}
                = $metaInfo{full_aperture_calibration};
    }

    if (exists($metadata{"Gap Phase Source"}))
    {
        $metadata{"Gap Phase Source"} = $metaInfo{gap_phase_source};
    }

    if (exists($metadata{"Gap Phase Statistic"}))
    {
        $metadata{"Gap Phase Statistic"} = $metaInfo{gap_phase_statistic};
    }

    # bands - some sensors have more gain bands, may have gain change bands
    # and some have quality bands, so check for each possible combination
    my @bands =('1', '2', '3', '4', '5', '6', '6 VCID 1', '6 VCID 2', '7', '8');
    foreach my $band (@bands)
    {
        # fieldnames end like _6, _6_vcid_1, _6_vcid_2
        my $fieldExt = lc($band); # convert the band string to lowercase
           $fieldExt =~ s/\s/_/g; # replace spaces with underscores

        if (exists($metadata{"Gain Band $band"}))
        {
            $metadata{"Gain Band $band"}
                = $metaInfo{"gain_band_$fieldExt"};
        }
        if (exists($metadata{"Gain Change Band $band"}))
        {
            $metadata{"Gain Change Band $band"}
                = $metaInfo{"gain_change_band_$fieldExt"};
        }
        if (exists($metadata{"Quality Band $band"}))
        {
            $metadata{"Quality Band $band"}
                = $metaInfo{"quality_band_$fieldExt"};
        }
    }

    # Level 1 metadata - this field selects whether to display L1 fields
    if (defined($metaInfo{data_type_l1})) # new naming convention
    {
        $metadata{"Data Type Level 1"}
            = format_data_type_level_1($metaInfo{data_type_l1});
    }
    elsif (defined($metaInfo{data_type_level_1})) # old naming convention
    {
        $metadata{"Data Type Level 1"}
            = format_data_type_level_1($metaInfo{data_type_level_1});
    }
    else
    {
        $metadata{"Data Type Level 1"} = '';
    }

    # If there is Level 1 to display:
    if ($metadata{"Data Type Level 1"}
        && $metadata{"Data Type Level 1"} !~ /require/i)
    {
        $display_level_1 = 1;

        # Give all the fields in @l1order a value so we know to expect data
        @metadata{@l1order} = ("") x scalar(@l1order);

        # Some Level 1 fields are the same across all Landsat sensors
        $metadata{"Datum"} = $metaInfo{datum};
        $metadata{"Elevation Source"} = $metaInfo{elevation_source};
        $metadata{"Ellipsoid"} = $metaInfo{ellipsoid};
        $metadata{"Ephemeris Type"} = $metaInfo{ephemeris_type};
        $metadata{"False Easting"} = $metaInfo{false_easting};
        $metadata{"False Northing"} = $metaInfo{false_northing};
        $metadata{"Geometric RMSE Model"}
            = $metaInfo{geometric_rmse_model};
        $metadata{"Geometric RMSE Model X"}
            = $metaInfo{geometric_rmse_model_x};
        $metadata{"Geometric RMSE Model Y"}
            = $metaInfo{geometric_rmse_model_y};
        $metadata{"Ground Control Points Model"}
            = $metaInfo{ground_control_points_model};
        $metadata{"Map Projection L1"} = $metaInfo{map_projection_l1};
        $metadata{"Orientation"} = $metaInfo{orientation};
        $metadata{"Output Format"} = $metaInfo{output_format};
        $metadata{"Resampling Option"} = $metaInfo{resampling_option};
# HACK: I accidentally ingested them as true_scale_lon for a while...
if (defined($metaInfo{true_scale_lat}))
{
        $metadata{"True Scale Latitude"} = $metaInfo{true_scale_lat};
}
elsif (defined($metaInfo{true_scale_lon}))
{
    $metadata{"True Scale Latitude"} = $metaInfo{true_scale_lon};
}
else
{
    $metadata{"True Scale Latitude"} = "";
}
        $metadata{"UTM Zone"} = $metaInfo{utm_zone};
        $metadata{"Vertical Longitude from Pole"}
            = $metaInfo{vertical_lon_from_pole};

        # All L1 have reflective; panchromatic and thermal vary, below
        $metadata{"Reflective Lines"} = $metaInfo{reflective_lines};
        $metadata{"Reflective Samples"} = $metaInfo{reflective_samples};
        $metadata{"Grid Cell Size Reflective"}
            = $metaInfo{grid_cell_size_reflective};

        # Only display one set of corners, so remove L0 corners
        # but only if the metadata has product corners (esp. during re-ingest).
        # (Assume presence of upper left lat indicates presence of all 8.)
        if (defined($metaInfo{corner_ul_lat_product}))
        {
            delete @metadata{"Corner Upper Left Latitude",
                             "Corner Upper Left Longitude",
                             "Corner Upper Right Latitude",
                             "Corner Upper Right Longitude",
                             "Corner Lower Right Latitude",
                             "Corner Lower Right Longitude",
                             "Corner Lower Left Latitude",
                             "Corner Lower Left Longitude"};
            $metadata{"Corner Upper Left Latitude Product"}
                = format_DMS_lat($metaInfo{corner_ul_lat_product});
            $metadata{"Corner Upper Left Longitude Product"}
                = format_DMS_lon($metaInfo{corner_ul_lon_product});
            $metadata{"Corner Upper Right Latitude Product"}
                = format_DMS_lat($metaInfo{corner_ur_lat_product});
            $metadata{"Corner Upper Right Longitude Product"}
                = format_DMS_lon($metaInfo{corner_ur_lon_product});
            $metadata{"Corner Lower Right Latitude Product"}
                = format_DMS_lat($metaInfo{corner_lr_lat_product});
            $metadata{"Corner Lower Right Longitude Product"}
                = format_DMS_lon($metaInfo{corner_lr_lon_product});
            $metadata{"Corner Lower Left Latitude Product"}
                = format_DMS_lat($metaInfo{corner_ll_lat_product});
            $metadata{"Corner Lower Left Longitude Product"}
                = format_DMS_lon($metaInfo{corner_ll_lon_product});
        }

        # Some fields may or may not exist, depending on sensor
        if (exists($metadata{"Ground Control Points Verify"}))
        {
            $metadata{"Ground Control Points Verify"}
                = $metaInfo{ground_control_points_verify};
        }
        if (exists($metadata{"Geometric RMSE Verify"}))
        {
            $metadata{"Geometric RMSE Verify"}
                = $metaInfo{geometric_rmse_verify};
        }

        # Thermal fields are in all sensors' L1; may have Panchromatic, Thermal
        if (exists($metadata{"Panchromatic Lines"}))
        {
            $metadata{"Panchromatic Lines"} = $metaInfo{panchromatic_lines};
        }
        if (exists($metadata{"Panchromatic Samples"}))
        {
            $metadata{"Panchromatic Samples"} = $metaInfo{panchromatic_samples};
        }
        if (exists($metadata{"Grid Cell Size Panchromatic"}))
        {
            $metadata{"Grid Cell Size Panchromatic"}
                = $metaInfo{grid_cell_size_panchromatic};
        }
        if (exists($metadata{"Thermal Lines"}))
        {
            $metadata{"Thermal Lines"} = $metaInfo{thermal_lines};
        }
        if (exists($metadata{"Thermal Samples"}))
        {
            $metadata{"Thermal Samples"} = $metaInfo{thermal_samples};
        }
        if (exists($metadata{"Grid Cell Size Thermal"}))
        {
            $metadata{"Grid Cell Size Thermal"}
                = $metaInfo{grid_cell_size_thermal};
        }

        # These all deal with gap-filling the SLC-off data
        if (exists($metadata{"Date Acquired Gap Fill"}))
        {
            # We don't reformat this like we do for Date Acquired
            # because there may be multiple images selected for gap-filling
            # (the value might be a list)
            $metadata{"Date Acquired Gap Fill"}
                = $metaInfo{date_acquired_gap_fill};
        }
        if (exists($metadata{"Gap Fill"}))
        {
            $metadata{"Gap Fill"} = $metaInfo{gap_fill};
        }
        if (exists($metadata{"Scan Gap Interpolation"}))
        {
            $metadata{"Scan Gap Interpolation"}
                = $metaInfo{scan_gap_interpolation};
        }
    }

    # glossary page for all Landsat definitions
    my $page = "landsat_dictionary.html#";
    
    # Everything's populated! Generate the table for the metadata.
    print "<CENTER>\n";
    print "<TABLE BORDER=\"1\" CELLPADDING=\"2\">\n";
    print Tr({-align=>"CENTER",-bgcolor=>"#006666"},
             td(font({-color=>"#FFFFCC"},"Dataset Attribute")),
             td(font({-color=>"#FFFFCC"},"Attribute Value")));
    foreach my $metaItem (@order) # @order varies by sensor
    {   
        # if we got rid of a field (after we initialized them all), skip it
        next if (!exists $metadata{$metaItem});

        # if a $metaInfo{} key did not exist, $metadata{} value may be undef
        $metadata{$metaItem} = "&nbsp;" if (!defined($metadata{$metaItem})
                                            || $metadata{$metaItem} =~ /^\s*$/);

        # a check for the metaItem to see if it needs a link to the
        # glossary page.
        if (!exists($glossaryLinks{$metaItem}))
        {
            print "<tr><td>$metaItem</td>";
            print "<td>$metadata{$metaItem}"
                  ."</td></tr>\n";
        }
        else
        {
            print "<tr><td><a href=\"#\" ";
            print "onClick=\"loadmeta('$glossaryLinks{$metaItem}','$page');\">";
            print "$metaItem</a></td>";
            print "<td>$metadata{$metaItem}</td></tr>\n";
        }
    }
    if ($display_level_1)
    {
        foreach my $metaItem (@l1order) # @order varies by sensor
        {   
            # if a Level 1 $metaInfo{} key had no data, don't display the row
            if (!defined($metadata{$metaItem})
                || $metadata{$metaItem} =~ /^\s*$/)
            {
                next;
            }

            # a check for the metaItem to see if it needs a link to the
            # glossary page.
            if (!exists($glossaryLinks{$metaItem}))
            {
                print "<tr><td>$metaItem</td>";
                print "<td>$metadata{$metaItem}"
                      ."</td></tr>\n";
            }
            else
            {
                print "<tr><td><a href=\"#\" ";
                print "onClick=\"loadmeta('$glossaryLinks{$metaItem}','$page');\">";
                print "$metaItem</a></td>";
                print "<td>$metadata{$metaItem}</td></tr>\n";
            }
        }
    }
    print "\n</TABLE>\n";
    print "</CENTER>\n";
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
    my $collection_id = '';
    my $ee_url = "http://earthexplorer.usgs.gov";

    if ($sensor == 8)
    {
        $collection_id = '4601';
    }
    elsif ($sensor == 7 && $is_slc_off)
    {
        $collection_id = '3373'
    }
    elsif ($sensor == 7)
    {
        $collection_id = "3372";
    }
    elsif ((($sensor == 4) || ($sensor == 5)) && ($mode eq "T"))
    {
        $collection_id = "3119";
    }
    elsif ((($sensor == 4) || ($sensor == 5)) && ($mode eq "M"))
    {
        $collection_id = "3120";
    }
    elsif (($sensor == 1) || ($sensor == 2) || ($sensor == 3))
    {
        $collection_id = "3120";
    }

    if ($collection_id)
    {
        print "<p><p align=center><a href=\"#\" ";
        print "onClick=\"loadfgdcmeta('$ee_url/form/fgdcmetadatalookup?collection_id=$collection_id&entity_id=$sceneid&primary_key=$sceneid&pageView=1');\">";
        print "FGDC Metadata</a></p>";
    }
    # Else do not provide a metadata link
}


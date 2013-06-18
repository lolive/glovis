#!/usr/bin/perl -wT
# This cgi script implements a search utility to find a scene ID in the
# inventory for the sensor & scene id passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# remove $ENV when working if possible
$ENV{'PATH'} = '/bin:/usr/bin:/usr/local/bin';

# hash holding all landsat or landsat-derivative sensor or dataset dir names
my %landsatDir = ("LANDSAT_8"=>"l8oli",
                  "LANDSAT_ETM"=>"l7",
                  "LANDSAT_ETM_SLC_OFF"=>"l7slc_off", 
                  "LANDSAT_TM"=>"l5",
                  "LANDSAT_MSS"=>"l4_5mss",
                  "LANDSAT_MSS4_5"=>"l4_5mss",
                  "LANDSAT_MSS1_3"=>"l1_3mss",
                  "LANDSAT_1_3MSS"=>"l1_3mss",
                  "ORTHO_MSS_SCENE"=>"ortho/mss",
                  "MRLC2K_ARCH"=>"mrlc_2001_tc",
                  "MRLC2K_SITC"=>"mrlc_2001_ra",
                  "ESAT_TM"=>"ortho/tm",
                  "ESAT_ETM_NOPAN"=>"ortho/etm",
                  "ESAT_ETM_PAN"=>"ortho/pansharp_etm",
                  "GLS2010"=>"gls/gls2010",
                  "GLS2010_EO1"=>"gls/gls2010_eo1",
                  "GLS2005"=>"gls/gls2005",
                  "GLS2005_EO1"=>"gls/gls2005_eo1",
                  "GLS2000"=>"gls/gls2000",
                  "GLS1990"=>"gls/gls1990",
                  "GLS1975_4_5MSS"=>"gls/gls1975_mss4_5",
                  "GLS1975_1_3MSS"=>"gls/gls1975_mss1_3",
                  "SYSTEMATIC_L1G"=>"lsat_sys"
                 );

# hash holding all the MODIS product directory names
my %modisDir = ("MOD09A1"=>"modis/mod09a1",
                "MOD09GA"=>"modis/mod09ga",
                "MOD09GQ"=>"modis/mod09gq",
                "MOD09Q1"=>"modis/mod09q1",
                "MOD11A1_DAY"=>"modis/mod11a1_day",
                "MOD11A1_NIGHT"=>"modis/mod11a1_night",
                "MOD11A2_DAY"=>"modis/mod11a2_day",
                "MOD11A2_NIGHT"=>"modis/mod11a2_night",
                "MOD11B1_DAY"=>"modis/mod11b1_day",
                "MOD11B1_NIGHT"=>"modis/mod11b1_night",
                "MOD13A1_EVI"=>"modis/mod13a1_evi",
                "MOD13A1_NDVI"=>"modis/mod13a1_ndvi",
                "MOD13A2_EVI"=>"modis/mod13a2_evi",
                "MOD13A2_NDVI"=>"modis/mod13a2_ndvi",
                "MOD13A3_EVI"=>"modis/mod13a3_evi",
                "MOD13A3_NDVI"=>"modis/mod13a3_ndvi",
                "MOD13Q1_EVI"=>"modis/mod13q1_evi",
                "MOD13Q1_NDVI"=>"modis/mod13q1_ndvi",
                "MOD14A1"=>"modis/mod14a1",
                "MOD14A2"=>"modis/mod14a2",
                "MOD15A2_FPAR"=>"modis/mod15a2_fpar",
                "MOD15A2_LAI"=>"modis/mod15a2_lai",
                "MOD17A2_GPP"=>"modis/mod17a2_gpp",
                "MOD17A2_NETPSN"=>"modis/mod17a2_netpsn",
                "MOD17A3_GPP"=>"modis/mod17a3_gpp",
                "MOD17A3_NPP"=>"modis/mod17a3_npp",
                "MOD44B_VCF"=>"modis/mod44b_vcf",
                "MYD09A1"=>"modis/myd09a1",
                "MYD09GA"=>"modis/myd09ga",
                "MYD09GQ"=>"modis/myd09gq",
                "MYD09Q1"=>"modis/myd09q1",
                "MYD11A1_DAY"=>"modis/myd11a1_day",
                "MYD11A1_NIGHT"=>"modis/myd11a1_night",
                "MYD11A2_DAY"=>"modis/myd11a2_day",
                "MYD11A2_NIGHT"=>"modis/myd11a2_night",
                "MYD11B1_DAY"=>"modis/myd11b1_day",
                "MYD11B1_NIGHT"=>"modis/myd11b1_night",
                "MYD13A1_EVI"=>"modis/myd13a1_evi",
                "MYD13A1_NDVI"=>"modis/myd13a1_ndvi",
                "MYD13A2_EVI"=>"modis/myd13a2_evi",
                "MYD13A2_NDVI"=>"modis/myd13a2_ndvi",
                "MYD13A3_EVI"=>"modis/myd13a3_evi",
                "MYD13A3_NDVI"=>"modis/myd13a3_ndvi",
                "MYD13Q1_EVI"=>"modis/myd13q1_evi",
                "MYD13Q1_NDVI"=>"modis/myd13q1_ndvi",
                "MYD14A1"=>"modis/myd14a1",
                "MYD14A2"=>"modis/myd14a2",
                "MYD15A2_FPAR"=>"modis/myd15a2_fpar",
                "MYD15A2_LAI"=>"modis/myd15a2_lai",
                "MYD17A2_GPP"=>"modis/myd17a2_gpp",
                "MYD17A2_NETPSN"=>"modis/myd17a2_netpsn",
                "MCD15A2_LAI"=>"modis/mcd15a2_lai",
                "MCD15A2_FPAR"=>"modis/mcd15a2_fpar",
                "MCD15A3_LAI"=>"modis/mcd15a3_lai",
                "MCD15A3_FPAR"=>"modis/mcd15a3_fpar",
                "MCD43A1"=>"modis/mcd43a1",
                "MCD43A2"=>"modis/mcd43a2",
                "MCD43A3"=>"modis/mcd43a3",
                "MCD43A4"=>"modis/mcd43a4",
                "MCD43B1"=>"modis/mcd43b1",
                "MCD43B2"=>"modis/mcd43b2",
                "MCD43B3"=>"modis/mcd43b3",
                "MCD43B4"=>"modis/mcd43b4"
               );

# hash holding the EO-1 product directory names
my %eo1Dir = ("EO1_ALI_PUB"=>"eo1/ali",
              "EO1_HYP_PUB"=>"eo1/hyp",
              "EO1_ALI"=>"eo1/ali",
              "EO1_HYP"=>"eo1/hyp"
             );

# let the web server know that the output will be plain text
print "Content-type: text/plain\n\n";

# get the sceneid & sensors passed in (sensor is same as EE dataset_name)
my @tainted_sensors = param('sensor');
my $tainted_sceneid = param('scene_id');

unless ($tainted_sceneid)
{
    print "Error: No scene ID\n";
    exit;
}

# unencode the scene id
$tainted_sceneid =~ s/%(..)/pack("c",hex($1))/ge;

# untaint the sensors
my @sensors = ();
foreach my $tainted_sensor (@tainted_sensors)
{
    # untaint the sensors
    unless($tainted_sensor =~ m#^([\w.-]+)$#)
    {
        print "Error: CGI script error\n";
        exit;
    }

    # translate the TerraLook sensors to the correct names
    my $temp_sensor = $1;
    if ($temp_sensor eq "TERRA_GLS2010")
    {
        $temp_sensor = "GLS2010";
    }
    if ($temp_sensor eq "TERRA_GLS2005")
    {
        $temp_sensor = "GLS2005";
    }
    elsif ($temp_sensor eq "TERRA_GLS2000")
    {
        $temp_sensor = "GLS2000";
    }
    elsif ($temp_sensor eq "TERRA_GLS1990")
    {
        $temp_sensor = "GLS1990";
    }
    elsif ($temp_sensor eq "TERRA_GLS1975_L4_5")
    {
        $temp_sensor = "GLS1990";
    }
    elsif ($temp_sensor eq "TERRA_GLS1975_L1_3")
    {
        $temp_sensor = "GLS1990";
    }
    elsif ($temp_sensor eq "TERRA_ASTER")
    {
        $temp_sensor = "ASTER_VNIR";
    }
    push(@sensors, $temp_sensor);
}

# remove a "AST_L1A.###:" from the beginning of ASTER sceneid if it is there
if ($tainted_sceneid =~ m/(AST_L1[AB]\.\d{3}:)(\d{5,10})/)
{
    $tainted_sceneid = $2;
}

# remove a "SC:M?D????.###:" from the beginning of MODIS sceneid if it is there
if ($tainted_sceneid =~ m/(M[YOC]D[\d\w]{3,4}\.\d{3}:)(\d{5,10})/)
{
    $tainted_sceneid = $2;
}

# untaint the scene id
unless($tainted_sceneid =~ m#^([\w\.\-]+)$#)
{
    # if the untainting failed, the ID is invalid
    print "Invalid: Invalid ID\n";
    exit;
}
my $sceneid = $1;

my $gridCol;
my $gridRow;

# search each of the datasets specified
my $sensor;
my $valid_found = 0;
my $projCode;
my $tocLine;
my $scene_found = 0;
foreach my $currSensor (@sensors)
{
    $sensor = $currSensor;

    # start to validate the sceneid that was passed in
    my $valid = validID($sensor,$sceneid);
    if ($valid >= 1)
    {
        $valid_found = 1;
        my $tocFile;

        # for landsat sensors or datasets
        if ($landsatDir{$sensor})
        {
            $tocFile = getLandsatTocFilename();
            ($projCode, $tocLine) = searchTocFile($tocFile);

            $gridCol = int($gridCol);
            $gridRow = int($gridRow);
        }
        elsif ($eo1Dir{$sensor})
        {
            $tocFile = getEO1TocFilename();
            ($projCode, $tocLine) = searchTocFile($tocFile);
            $gridCol = int($gridCol);
            $gridRow = int($gridRow);
        }
        elsif ($modisDir{$sensor} && $valid == 1)
        {
            # The MODIS primary ID lets us go right to the TOC file
            $tocFile = getModisTocFilename();
            ($projCode, $tocLine) = searchTocFile($tocFile);
            $gridCol = int($gridCol);
            $gridRow = int($gridRow);
        }
        elsif ($sensor eq "NALC")
        {
            $gridCol = substr($sceneid, 6, 3);
            $gridRow = substr($sceneid, 9, 3);
            $tocFile = "nalc/p$gridCol/r$gridRow/TOC";
            ($projCode, $tocLine) = searchTocFile($tocFile);
            $gridCol = int($gridCol);
            $gridRow = int($gridRow);
        }
        elsif ($sensor eq "ORTHO_MOSAIC")
        {
            $gridCol = substr($sceneid, 4, 2);
            $gridRow = substr($sceneid, 2, 1) . substr($sceneid, 7, 2);
            $tocFile = "ortho/tm_mosaic/$gridCol/$gridRow/TOC";

            # a few scenes in the TM collection have two rows in their
            # name, so they both need to be checked
            if ($sceneid =~ /^\w{3}-\d{2}-\d{2}_\d{2}_\w{3}$/)
            {
                if (! -e $tocFile)
                {
                    $gridRow = substr($sceneid, 2, 1)
                             . substr($sceneid, 10, 2);
                    $tocFile = "ortho/tm_mosaic/$gridCol/$gridRow/TOC";
                }
            }
            ($projCode, $tocLine) = searchTocFile($tocFile);
        }
        elsif ($sensor eq "ORTHO_MOSAIC_ETM")
        {
            $gridCol = substr($sceneid, 4, 2) . substr($sceneid, 11, 1);
            $gridRow = substr($sceneid, 2, 1) . substr($sceneid, 7, 2)
                     . substr($sceneid, 10, 1);
            $tocFile = "ortho/etm_mosaic/$gridCol/$gridRow/TOC";
            ($projCode, $tocLine) = searchTocFile($tocFile);
        }
        else
        {
            # for the LIMA, ASTER, and NAPP datasets, find the grid
            # column and row from the lookup file
            # also for the MODIS dataset if we are using the secondary ID
            my $csvFile;
            if ($sensor =~ /NAPP/)
            {
                # NAPP dataset
                $tocFile = "napp";
                $csvFile = "napp/napp_lookup.csv";
            }
            elsif ($sensor =~ /NHAP/)
            {
                # NHAP dataset
                if (substr($sceneid, 1, 1) eq "B")
                {
                    $tocFile = "nhap/bw";
                }
                else
                {
                    $tocFile = "nhap/cir";
                }
                $csvFile = "$tocFile/nhap_lookup.csv";
            }
            elsif ($sensor =~ /LIMA/)
            {
                $tocFile = "lima";
                $csvFile = "lima/lookup.csv";
            }
            elsif ($sensor =~ /ASTER_VNIR_DATAPOOL/)
            {
                $tocFile = "aster_datapool/vnir";
                $csvFile = "aster_datapool/vnir/vnir_wrs_lookup.csv";
            }
            elsif ($sensor =~ /ASTER_TIR_DATAPOOL/)
            {
                $tocFile = "aster_datapool/tir";
                $csvFile = "aster_datapool/tir/tir_wrs_lookup.csv";
            }
            elsif ($sensor =~ /ASTER_VNIR/)
            {
                $tocFile = "aster/vnir";
                $csvFile = "aster/vnir/vnir_wrs_lookup.csv";
            }
            elsif ($sensor =~ /ASTER_TIR/)
            {
                $tocFile = "aster/tir";
                $csvFile = "aster/tir/tir_wrs_lookup.csv";
            }
            elsif ($sensor =~ /^M[YOC]D[\d\w]{3,4}/ && $modisDir{$sensor})
            {
                $tocFile = $modisDir{$sensor}; # will finish the file name below
                $csvFile =$modisDir{$sensor}.'/'.lc($sensor)."_grid_lookup.csv";
            }
            else
            {
                $tocFile = "";
                $csvFile = "";
            }

            ($gridCol,$gridRow) = openCSV($csvFile);
           
            if (($sensor =~ /NAPP/) || ($sensor =~ /NHAP/))
            {
                # use the gridCol and gridRow as read
                $tocFile .= "/$gridCol/$gridRow/TOC";
            }
            elsif ($sensor =~ /LIMA/)
            {
                $tocFile .= "/$gridCol/$gridRow/TOC";
            }
            elsif ($sensor =~ /^M[YOC]D[\d\w]{3,4}/)
            {
                # make sure the gridCol and gridRow have 2 digits
                $gridCol = sprintf("%02d",$gridCol);
                $gridRow = sprintf("%02d",$gridRow);
                $tocFile .= "/h$gridCol/v$gridRow/TOC";
            }
            else
            {
                # check to make sure the gridCol and gridRow have 3 digits
                $gridCol = sprintf("%03d",$gridCol);
                $gridRow = sprintf("%03d",$gridRow);

                $tocFile .= "/p$gridCol/r$gridRow/TOC";
            }

            ($projCode, $tocLine) = searchTocFile($tocFile);
        }
        next if ($tocLine =~ /^Not Found/);
        next if ($tocLine =~ /^Error/);
        print "Found:$gridCol,$gridRow,$projCode,$sensor,$tocLine\n";
        $scene_found = 1;
        last;
    }
}
if (!$valid_found)
{
    print "Invalid: Invalid ID\n";
}
elsif (!$scene_found)
{
    print $tocLine;
}

exit;

# routine to validate the sceneid that is passed in
# returns 1 if valid, or 2 if valid but need to use "secondary" ID
sub validID
{
    my($sensor,$sceneid) = @_;
   
    # hash holding sceneid pattern match (most sensors)
    my %validSceneid =("LANDSAT_8"=>'L[OC]\d{14}\w{3}\d{2}',
              "LANDSAT_ETM"=>'LE\d{14}\w{3}\d{2}',
              "LANDSAT_ETM_SLC_OFF"=>'LE\d{14}\w{3}\d{2}',
              "LANDSAT_TM"=>'LT\d{14}\w{3}\d{2}',
              "LANDSAT_MSS"=>'LM\d{14}\w{3}\d{2}',
              "MRLC2K_ARCH"=>'L[ET]\d{14}[A-Z]{3}\d{2}|[A-Z]{3}\w\d{17}',
              "MRLC2K_SITC"=>'L[ET]\d{14}[A-Z]{3}\d{2}|[A-Z]{3}\w\d{17}',
              "ESAT_TM"=>'\D{2}P\d{3}R\d{2}_\d{1}\D{1}\d{8}',
              "ORTHO_MSS_SCENE"=>'\D{2}P\d{3}R\d{2}_\d{1}\D{1}\d{8}',
              "ESAT_ETM_NOPAN"=>'\D{2}P\d{3}R\d{3}_\d{1}\D{1}\d{8}',
              "ESAT_ETM_PAN"=>'\D{2}P\d{3}R\d{3}_\d{1}\D{1}\d{8}',
              "ASTER_TIR"=>'\d{5,10}',
              "ASTER_VNIR"=>'\d{5,10}',
              "ASTER_TIR_DATAPOOL"=>'\d{5,10}',
              "ASTER_VNIR_DATAPOOL"=>'\d{5,10}',
              "EO1_ALI_PUB"=>'EO1A\d{16}[\w_]+',
              "EO1_HYP_PUB"=>'EO1H\d{16}[\w_]+',
              "EO1_ALI"=>'EO1A\d{16}[\w_]+',
              "EO1_HYP"=>'EO1H\d{16}[\w_]+',
              "NAPP"=>'N\w0NAPP\w\d{8}',
              "NHAP"=>'N\w{2}NHAP\w\d{8}',
              "NALC"=>'LPNALC\d{6}\w',
              "ORTHO_MOSAIC"=>'^\w{3}-\d{2}-\d{2}_\w{3,7}$',
              "ORTHO_MOSAIC_ETM"=>'^\w{3}-\d{2}-\d{2}_\w{3,7}$',
              "GLS2010"=>'L[ET]\d{14}\w{3}\d{2}',
              "GLS2010_EO1"=>'EO1A\d{16}[\w_]+',
              "GLS2005"=>'L[ET]\d{14}\w{3}\d{2}',
              "GLS2005_EO1"=>'EO1A\d{16}[\w_]+',
              "GLS2000"=>'P\d{3}R\d{3}_\d\w\d{8}',
              "GLS1990"=>'P\d{3}R\d{3}_\d\w\d{8}',
              "GLS1975_4_5MSS"=>'P\d{3}R\d{3}_\d\w\d{8}',
              "GLS1975_1_3MSS"=>'P\d{3}R\d{3}_\d\w\d{8}',
              "SYSTEMATIC_L1G"=>'L71\d{6}_\d{11}',
              "LIMA"=>'LE\d{16}',
              );

    my $valid_pattern = $validSceneid{$sensor};
    if (defined($valid_pattern))
    {
        if ($sceneid =~ m/^$valid_pattern$/)
        {
            return 1;
        }
    }
    my $checkModis = substr($sensor,0,3);
    if (($checkModis eq "MOD") || ($checkModis eq "MYD") 
        || ($checkModis eq "MCD"))
    {
        if ($sceneid =~ m/^[\w.-]+h\d{2}v\d{2}[\w.-]+$/)
        {
            return 1; # primary ID (local Granule ID), tells the path/row
        }
        elsif ($sceneid =~ m/^\d{5,10}$/)
        {
            return 2; # use secondary ID: search CSV file to get path/row
        }
    }
    return 0;
}

# routine to build the TOC filename for Landsat related datasets
sub getLandsatTocFilename
{
    # extract the components needed from the scene id to build the path to the 
    # TOC file
    if (($sensor eq "LANDSAT_8") ||
        ($sensor eq "LANDSAT_ETM") || ($sensor eq "LANDSAT_ETM_SLC_OFF") ||  
        ($sensor eq "LANDSAT_TM") || ($sensor =~ /LANDSAT_.*MSS.*/) ||
        ($sensor eq "GLS2010") || ($sensor eq "GLS2005") ||
        ($sensor eq "SYSTEMATIC_L1G"))
    {
        $gridCol = substr($sceneid,3,3);
        $gridRow = substr($sceneid,6,3);
        # if MSS, tack on something to make the sensor unique for the dataset
        if ($sensor =~ /LANDSAT_.*MSS.*/)
        {
            # Standardize the MSS sensor names (we accept several variations)
            if (int(substr($sceneid,2,1)) <= 3)
            {
                $sensor = "LANDSAT_MSS1_3";
            }
            else
            {
                $sensor = "LANDSAT_MSS4_5";
            }
        }
    }
    elsif (($sensor eq "GLS2000") || ($sensor =~ /^GLS19/))
    {
        $gridCol = substr($sceneid,1,3);
        $gridRow = substr($sceneid,5,3);
    }
    elsif (($sensor eq "MRLC2K_SITC") || ($sensor eq "MRLC2K_ARCH"))
    {
        # MRLC can have an ID just like a Landsat ID
        if ($sceneid =~ /L[ET]\d{14}[A-Z]{3}\d{2}/)
        {
            $gridCol = substr($sceneid,3,3);
            $gridRow = substr($sceneid,6,3);
        }
        elsif ($sceneid =~ /[A-Z]{3}\w\d{17}/) # or an MRLC ID
        {
            $gridCol = substr($sceneid,5,3);
            $gridRow = substr($sceneid,8,3);
        }
    }
    elsif (($sensor eq "ESAT_ETM_NOPAN") || ($sensor eq "ESAT_ETM_PAN")) 
    {
        $gridCol = substr($sceneid,3,3);
        $gridRow = substr($sceneid,7,3);
    }
    elsif (($sensor eq "ESAT_TM") || ($sensor eq "ORTHO_MSS_SCENE"))
    {
        $gridCol = substr($sceneid,3,3);
        $gridRow = substr($sceneid,7,2);
        #Note: the gridRow is only a two digit value therefore we need to add a
        #"0" value to the left of the value to optain the correct directory name
        $gridRow = "0".$gridRow;
    }
    elsif ($sensor eq "GLS2005_EO1" || $sensor eq "GLS2010_EO1")
    {
        $gridCol = substr($sceneid,4,3);
        $gridRow = substr($sceneid,7,3);
    }

    # build the location of the directory and file:
    # (sensor/gridCol/gridRow/TOC) for were the toc is located
    my $sensorpath = $landsatDir{$sensor};

    # for ortho mss, there are separate subdirectorys for 1-3 vs 4-5
    if ($sensor eq "ORTHO_MSS_SCENE")
    {
        if (int(substr($sceneid,10,1)) <= 3)
        {
            $sensorpath .= "1_3";
        }
        else
        {
            $sensorpath .= "4_5";
        }
    }

    my $filename = $sensorpath."/p".$gridCol."/r".$gridRow."/TOC";

    return $filename;
}

# routine to build the TOC filename for EO-1 related datasets
sub getEO1TocFilename
{
    $gridCol = substr($sceneid,4,3);
    $gridRow = substr($sceneid,7,3);

    # build the location of the directory and file:
    # (sensor/gridCol/gridRow/TOC) for were the toc is located
    my $sensorpath = $eo1Dir{$sensor};
    my $filename = $sensorpath."/p".$gridCol."/r".$gridRow."/TOC";

    return $filename;
}

# routine for MODIS products
sub getModisTocFilename
{
    # extract the components needed from the scene id to build the path to the 
    # TOC file
    
    my @hTile = split(/h/, $sceneid);
    my @vTile = split(/v/, $sceneid);
    $gridCol = substr($hTile[1],0,2);
    $gridRow = substr($vTile[1],0,2);
        
    # build the the location of the directory and file
    my $sensorpath = $modisDir{$sensor};
    my $filename = $sensorpath."/h".$gridCol."/v".$gridRow."/TOC";
    
    return $filename;
}

# routine to search a TOC file for a matching scene
sub searchTocFile
{
    my ($tocfile) = @_;
    my $line;
    my $count = 0;

    # If TOC file doesn't exist return a not found message
    if (!-e $tocfile)
    {
        return 1, "Not Found: Matching scene ID not found\n";
    }

    # read the first line from the TOC file to get the projection code
    open(TOC, "<$tocfile") or die "Error: Search failed";
    my $firstLine = <TOC>;
    close(TOC);
    chomp $firstLine;
    my @data = split(',', $firstLine);
    if ($#data < 3)
    {
        print "Error: Unable to read TOC file\n";
        exit;
    }
    my $projCode = $data[2];

    # grep the TOC file for the corresponding sceneid
    open (TOC,"/bin/grep '$sceneid' $tocfile|")
        or die "Error: Search failed";
    
    while (<TOC>) 
    {
        $line = $_;
        chomp($line);
        $count++;
    }
    close (TOC);

    # if no lines were returned from the grep statement therefore the 
    # sceneid was not in the TOC file
    if ($count == 0)
    {
        return 1, "Not Found: Matching scene ID not found\n";
    }
    # making sure that only one line was returned for the grep statement
    elsif ($count > 1)
    {
        print "Error: $count lines returned from grep statement\n";
        exit;
    }
    
    return $projCode, $line;
}

# routine to open csv (comma separated values) file and parse the correct
# information back to the search based on global $sceneid
sub openCSV
{   
    my($csvfile) = @_;
    my @values;
    my $line;
    my $count = 0;
    my $gridCol;
    my $gridRow;
    
    # if CSV file doesn't exist return error message
    if (!-e $csvfile)
    {
        print "Error: CSV file does not exist\n";
        exit;
    }
 
    # grep the CSV file for the corresponding sceneid
    open (CSV,"/bin/grep '$sceneid' $csvfile|")
        or die "Error: Search failed";
    
    # read the line from the CSV file
    while (<CSV>) 
    {
        $line = $_;
        chomp($line);
        @values = split /,/, $line;
        $count++;

        # pull the information from the selected line
        $gridCol = $values[1];
        $gridRow = $values[2];
    }
    close (CSV);
    
    # if no lines were returned from the grep statement therefore the 
    # sceneid was not in the CSV file
    if ($count == 0)
    {
        print "Not Found: Matching scene ID not found\n";
        exit;
    }
    # making sure that only one line was returned for the grep statement
    elsif ($count > 1)
    {
        print "Error: $count lines returned from grep statement\n";
        exit;
    }

    # untaint the gridCol and gridRow
    my $untaintedGridCol = "0";
    if ($gridCol =~ m#^([\w.-]+)$#)
    {
        $untaintedGridCol = $1;
    }
    my $untaintedGridRow = "0";
    if ($gridRow =~ m#^([\w.-]+)$#)
    {
        $untaintedGridRow = $1;
    }

    return ($untaintedGridCol,$untaintedGridRow); 
}


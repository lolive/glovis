#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

use strict;
use CGI qw(:standard escapeHTML);

my %l1tDir = ("GLS2010"        => "gls/gls2010",
              "GLS2010_EO1"    => "gls/gls2010_eo1",
              "GLS2005"        => "gls/gls2005",
              "GLS2005_EO1"    => "gls/gls2005_eo1",
              "GLS2000"        => "gls/gls2000",
              "GLS1990"        => "gls/gls1990",
              "GLS1975_4_5MSS" => "gls/gls1975_mss4_5",
              "GLS1975_1_3MSS" => "gls/gls1975_mss1_3",
              "LIMA"           => "lima",
              "SYSTEMATIC_L1G" => "lsat_sys"
             );

# get the scene id and dataset passed in
my $tainted_sceneid = param('scene_id');
my $tainted_dataset = param('dataset');

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# untaint the scene id
unless($tainted_sceneid =~ m#^([\w\.-]+)$#)
{
    handleerror("Bad Scene ID $tainted_sceneid");
}

# make sure it is really a scene id for one of the L1 datasets and extract
# some needed info from the scene id, determine sensorpath based on dataset
my $dataset;
my $sensorpath;
my $path;
my $row;
my $year;

if (($tainted_dataset =~ /^\w+$/) and $l1tDir{$tainted_dataset})
{
    $sensorpath = $l1tDir{$tainted_dataset};
    $dataset = $tainted_dataset;
}
else
{
    # not a recognized dataset
    handleerror("Unrecognized dataset $tainted_dataset");
}

if ($tainted_sceneid =~ /^P\d{3}R\d{3}_\d\w\d{8}$/)
{
    # GLS2000, GLS1990, GLS1975 (MSS 4-5 and MSS 1-3)
    $path = "p" . substr($tainted_sceneid,1,3);
    $row = "r" . substr($tainted_sceneid,5,3);
    $year = substr($tainted_sceneid,11,4);
}
elsif ($tainted_sceneid =~ /^EO1[AH]\d{16}\w{9}$/)
{
    # GLS2005 and GLS2010 Islands - EO-1
    $path = "p" . substr($tainted_sceneid,4,3);
    $row = "r" . substr($tainted_sceneid,7,3);
    $year = substr($tainted_sceneid,10,4);
}
elsif ($tainted_sceneid =~ /^L[ET]\d{14}\w{3}\d{2}$/)
{
    # GLS2005
    $path = "p" . substr($tainted_sceneid,3,3);
    $row = "r" . substr($tainted_sceneid,6,3);
    $year = substr($tainted_sceneid,9,4);
}
elsif ($tainted_sceneid =~ /^L71\d{6}\_\d{11}$/)
{
    # SYSTEMATIC_L1G
    $path = "p" . substr($tainted_sceneid,3,3);
    $row = "r" . substr($tainted_sceneid,6,3);
    $year = substr($tainted_sceneid,13,4);
}
elsif ($tainted_sceneid =~ /^L[ET]\d{16}$/)
{
    # LIMA
    my $tainted_path = param('path');
    my $tainted_row = param('row');
    if ($tainted_path =~ /^([EW]\d{1,3})$/)
    {
        $path = $1;
    }
    if ($tainted_row =~ /^([NS]\d{1,3})$/)
    {
        $row = $1;
    }
    $year = substr($tainted_sceneid,11,2);
    if (int($year) > 98)
    {
        $year = "19" . $year;
    }
    else
    {
        $year = "20" . $year;
    }
}
if (not defined $path)
{
    handleerror("No path, can't parse scene ID $tainted_sceneid");
}

# build the directory name for this sensor/path/row/year
my $dirname = $sensorpath . "/" . $path . "/" . $row . "/y" . $year;
my $sceneid = $tainted_sceneid;
my $image = "";

my $metafile="";

# Build the .meta file name
if ($dataset =~ /GLS2005_EO1|GLS2010_EO1/)
{
    $metafile = $dirname . "/" . $sceneid . "_0240.meta";
}
else
{
    $metafile = $dirname . "/" . $sceneid . ".meta";
}

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;

// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 790;
    winWidth = 390;

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

addFgdcMetadata($dataset);

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

# read the metadata from the file, format it nicely and according to
# the sensor, and print the html table
sub buildMetaTable
{
    my ($filename) = @_;
    my @order;
    my %metaInfo;   # metadata as read from the file
    my %metadata;   # metadata formatted for display
                    # where keys match values in the @order array

    #This is the order we want the items to appear in the table
    @order = ("Entity ID",
              "Scene Size",
              "Acquisition Date",
              "WRS-2 Path",
              "WRS-2 Row");
    push(@order, "Zone") if ($dataset ne "LIMA");
    push(@order, ("Upper Left Corner",
                  "Upper Right Corner",
                  "Lower Left Corner",
                  "Lower Right Corner"));
    push(@order, ("Scene Center")) if ($dataset =~ /^GLS/);
    push(@order, ("QA Percent Missing Data")) if ($dataset eq "GLS2000");
    push(@order, ("Sun Azimuth",
                  "Sun Elevation",
                  "Satellite Number",
                  "Orientation"));
    push(@order, ("Product Type"));
    push(@order, ("Resampling Technique",
                  "Datum"));
    push(@order, ("Gap-Fill Percent",
                  "Gap-Fill Acquisition Date(s)"))
        if ($dataset =~ /^GLS20(05|10)$/); # Only 2005 and 2010, not EO-1

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

    close (META);

    # assign the values to the metadata hash that is used to populate the table

    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    @metadata{@order} = ("") x scalar(@order);

    $metadata{"Entity ID"} = $metaInfo{EntityID};
    $metadata{"Scene Size"} = $metaInfo{file_size};
    $metadata{"Acquisition Date"} = $metaInfo{acquisition_date};
    $metadata{"Acquisition Date"} =~ s/-/\//g;
    $metadata{"WRS-2 Path"} = sprintf("%03d", $metaInfo{path});
    $metadata{"WRS-2 Row"} = sprintf("%03d", $metaInfo{row});
    $metadata{"Zone"} = $metaInfo{utm_zone};
    $metadata{"QA Percent Missing Data"} = $metaInfo{qa_percent_missing_data};
    $metadata{"Sun Azimuth"} = $metaInfo{sun_azimuth};
    $metadata{"Sun Elevation"} = $metaInfo{sun_elevation};
    $metadata{"Satellite Number"} = $metaInfo{spacecraft_id};
    $metadata{"Orientation"} = $metaInfo{orientation};
    $metadata{"Product Type"} = $metaInfo{product_type};
    $metadata{"Resampling Technique"} = $metaInfo{resampling_option};
    $metadata{"Datum"} = $metaInfo{datum};
    $metadata{"Gap-Fill Percent"}
      = (($metaInfo{gap_fill_percent} && $metaInfo{gap_fill_percent} ne "NULL")
      ?  sprintf("%1.1f%%", $metaInfo{gap_fill_percent})
      : "Not Applicable");
    $metadata{"Gap-Fill Acquisition Date(s)"}
      = (($metaInfo{gap_fill_acq_date} && $metaInfo{gap_fill_acq_date} ne "NULL")
      ? $metaInfo{gap_fill_acq_date}
      : "Not Applicable");
     
    # format upper-left coordinate values
    $metadata{"Upper Left Corner"}
            = format_DMS_lat($metaInfo{ul_lat}) . ", "
            . format_DMS_lon($metaInfo{ul_lon});

    # format upper-right coordinate values
    $metadata{"Upper Right Corner"}
            = format_DMS_lat($metaInfo{ur_lat}) . ", "
            . format_DMS_lon($metaInfo{ur_lon});

    # format lower-right coordinate values
    $metadata{"Lower Right Corner"}
            = format_DMS_lat($metaInfo{lr_lat}) . ", "
            . format_DMS_lon($metaInfo{lr_lon});

    # format lower-left coordinate values
    $metadata{"Lower Left Corner"}
            = format_DMS_lat($metaInfo{ll_lat}) . ", "
            . format_DMS_lon($metaInfo{ll_lon});

    # format scene center coordinate values
    $metadata{"Scene Center"}
            = format_DMS_lat($metaInfo{scene_center_lat}) . ", "
            . format_DMS_lon($metaInfo{scene_center_lon});

    # set the glossary page
    my $page = "glossary_gls.html#";
    
    # generate the table for the metadata
    print "<CENTER>\n";
    print "<TABLE BORDER=\"1\" CELLPADDING=\"2\">\n";
    print Tr({-align=>"CENTER",-bgcolor=>"#006666"},
             td(font({-color=>"#FFFFCC"},"Dataset Attribute")),
             td(font({-color=>"#FFFFCC"},"Attribute Value")));
    foreach my $metaItem (@order)
    {
        # if a $metaInfo{} key did not exist, $metadata{} value may be undef
        $metadata{$metaItem} = " " if (!defined($metadata{$metaItem})
                                            || $metadata{$metaItem} =~ /^\s*$/);

        # make the glossary link (all items have a glossary link based on
        # the item name
        my $glossaryLink = lc($metaItem);
        $glossaryLink =~ s/ /_/g;

        # output the info
        print "<tr><td><a href=\"#\" ";
        print "onClick=\"loadmeta('$glossaryLink','$page');\">";
        print "$metaItem</a></td>";
        print "<td>$metadata{$metaItem}"
              ."</td></tr>\n";
    }
    print "\n</TABLE>\n";
    print "</CENTER>\n";
}

# routine to generate an error page  
sub handleerror
{
    my @messages = @_; # For debug mainly - do not print details in prod
    print header();
    print start_html("USGS Global Visualization Viewer - Metadata Error");
    print p("Error generating metadata page");
    #foreach (@messages)
    #{
    #    print p(@_);
    #}
    print end_html();
    exit;
}

# routine to add link to FGDC metadata
sub addFgdcMetadata
{
    my $dataset = $_[0];
    my $collection_id = '';

    if ($dataset eq 'GLS2010')
    {
        $collection_id = '4345';
    }
    elsif ($dataset eq 'GLS2010_EO1')
    {
        $collection_id = '4840';
    }
    elsif ($dataset eq 'GLS2005_EO1')
    {
        $collection_id = '3512';
    }
    elsif ($dataset eq 'GLS2005')
    {
        $collection_id = '3132';
    }
    elsif ($dataset eq 'GLS2000')
    {
        $collection_id = '3131';
    }
    elsif ($dataset eq 'GLS1990')
    {
        $collection_id = '3352';
    }
    elsif ($dataset =~ /GLS1975/)
    {
        $collection_id = '3391';
    }
    elsif ($dataset eq 'SYSTEMATIC_L1G')
    {
        $collection_id = '3211';
    }

    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('http://earthexplorer.usgs.gov/form/fgdcmetadatalookup?collection_id=$collection_id&entity_id=$sceneid&primary_key=$sceneid&pageView=1');\">";
    print "FGDC Metadata</a></p>";
}


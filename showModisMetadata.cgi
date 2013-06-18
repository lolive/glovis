#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

use strict;
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $sceneid;
my $secondaryid = param('secondary_id');
my $taintedbrowsenumber = param('browse_number');
my $browsenumber = 0;
my $browsenumext = ''; # extension to file name if it has a browse number
my $dataset = lc(param('dataset'));

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# make sure it is really a MODIS secondary id
if ($secondaryid
    !~ /^\w{6,8}\.A\d{7,7}\.h\d{2,2}v\d{2,2}\.\d{3,3}\.\d{13,13}\.hdf$/)
{
    handleerror("Secondary Scene ID $secondaryid is not legal"); # exits
}

# make sure it is a valid MODIS dataset
if ($dataset =~ /^M\wD\d\d[\d\w]{1,2}(_\w{3,6})?$/)
{
    handleerror("Dataset $dataset is not legal"); #exits
}

# extract the components needed from the secondary id to build the path to the 
# metadata
my @idFields = split(/\./, $secondaryid);
my $year = substr($idFields[1],1,4);
my $gridcol = substr($idFields[2],0,3);
my $gridrow = substr($idFields[2],3,3);

# Variables to hold values for the FGDC page
my $acqDate;
my $LLLong; 
my $URLong;
my $ULLat;
my $LRLat;

# build the directory name for this sensor/gridcol/gridrow/year
my $dirname = "modis/" . $dataset . "/" . $gridcol . "/" . $gridrow . 
              "/y" . $year;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
if ($taintedsceneid =~ /^\.?(\d{3}):(\d{6,10})$/)
{
    $sceneid = $2;
}
elsif ($taintedsceneid =~ /^(\d{6,10})$/)
{
    $sceneid = $1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal"); # exits
}

# unencode the browse number
$taintedbrowsenumber =~ s/%(..)/pack("c",hex($1))/ge;

# make sure the browse number is untainted
if ($taintedbrowsenumber =~ /^(\d{1,2})$/)
{
    $browsenumber = $1;
}
else
{
    #handleerror("Browse Number $taintedbrowsenumber is not legal");
    # We need to handle the old style for a while, so just ignore the error
}
# if $browsenumber is 0, this is an old-style file, so leave $browsenumext
# empty, otherwise add underscore and browse number to file names
if ($browsenumber)
{
    $browsenumext = "_$browsenumber";
}

# Build the .meta file name
my $metafile = $dirname ."/".$secondaryid;
$metafile =~ s/\.hdf/$browsenumext\.meta/;

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;

// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    winHeight = 830;
    winWidth = 700;

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

# routine to convert Y/N flags into a full word description
sub Yes_No
{
    $_ = $_[0];
    return "Yes" if (m/^[Y|y]/);
    return "No";
}

# routine to convert D/N flags into a full word description
sub Day_Night
{
    $_ = $_[0];
    return "Day" if (m/^[D|d]/);
    return "Night" if (m/^[N|n]/);
    return "Both" if (m/^[B|b]/);
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

# routine to convert the science quality Expl. field value of a link 
# into a link that can be click on and view the html page.
sub create_link
{
    my $link = $_[0];
    my $string; 
    my @array = split(/\s/, $link);
    
    foreach my $word (@array) 
    {
        #find the link in the array.
        if ($word =~ /http\:\/\//)
        {
            $word = "<a href='$word' target='_blank'>$word</a>";
        }
        
        $string .= $word ." ";
    }

    return $string;
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

    # Metadata layout
    # This is the order we want the items to appear in the table
    @order = ("Local Granule ID",
              "Granule ID",
              "Acquisition Date",
              "Horizontal Tile",
              "Vertical Tile",
              "Upper Left Corner",
              "Upper Right Corner",
              "Lower Left Corner",
              "Lower Right Corner",
              "Range Beginning Date",
              "Range Ending Date",
              "Day or Night",
              "Science Quality",
              "Science Quality Explanation",
              "Automatic Quality",
              "Automatic Quality Explanation",
              "PGE Version",
              "Percent Missing Data"
             );
                 
    # This is a hash holding the links to the web page        
    %glossaryLinks = ("Local Granule ID" => "local_granule_id",
                      "Granule ID" => "no_link",
                      "Acquisition Date" => "acquisition_date",
                      "Horizontal Tile"=> "horizontal_tile",
                      "Vertical Tile" => "vertical_tile",
                      "Upper Left Corner" => "ul_corner_lat",
                      "Upper Right Corner" => "ur_corner_lat",
                      "Lower Left Corner" => "ll_corner_lat",
                      "Lower Right Corner" => "lr_corner_lat",
                      "Range Beginning Date" => "beginning_date",
                      "Range Ending Date" => "ending_date",
                      "Day or Night" => "day_night",
                      "Science Quality" => "science_qlty",
                      "Science Quality Explanation" => "science_qlty_expl",
                      "Automatic Quality" => "automatic_qlty",
                      "Automatic Quality Explanation" => "automatic_qlty_expl",
                      "PGE Version" => "pge_version",
                      "Percent Missing Data" => "missing_data",
                      "Cloud Cover" => "cloud_cover",
                      "Browse Type" => "no_link",
                     );
      
    # read the lines from the metadata file
    open (META, "<$filename") or handleerror("can't open $filename: $!");
    
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
    $acqDate = substr($metaInfo{AcqStartDate},5,2)."-".
               substr($metaInfo{AcqStartDate},8,2)."-".
               substr($metaInfo{AcqStartDate},0,4);
    $LLLong = $metaInfo{LLLong};
    $URLong = $metaInfo{URLong};
    $ULLat = $metaInfo{ULLat};
    $LRLat = $metaInfo{LRLat};

    # assign the values to the metadata hash that is used to populate the table

    # first, give every entry in @order a default value in %metadata
    # so that we know whether this sensor should expect a value for that field
    @metadata{@order} = ("") x scalar(@order);

    $metadata{"Range Beginning Date"} = $metaInfo{AcqStartDate};
  
    $metadata{"Range Ending Date"} = $metaInfo{AcqEndDate};

    $metadata{"Horizontal Tile"} = $metaInfo{HTileNum};
   
    $metadata{"Vertical Tile"} = $metaInfo{VTileNum};

    $metadata{"Day or Night"} = $metaInfo{DayNight};

    $metadata{"Science Quality"} = $metaInfo{ScienceQltyFlag};
  
    $metadata{"Automatic Quality"} = $metaInfo{AutoQltyFlag};

    $metadata{"Automatic Quality Explanation"} = $metaInfo{AutoQltyFlagExpl};

    $metadata{"PGE Version"} = $metaInfo{PGEVersion};

    $metadata{"Percent Missing Data"} = $metaInfo{MissingData};

    $metadata{"Local Granule ID"} = $metaInfo{entityID};

    $metadata{"Granule ID"} = $metaInfo{DBid};

    $metadata{"Acquisition Date"} = $metaInfo{AcqDate};

    # format the Science Qlty Expl. by making the link.
    $metadata{"Science Quality Explanation"} = create_link(
                                        $metaInfo{ScienceQltyFlagExpl});

    my $tmp;

    # format ULLat coordinate values
    $tmp = format_DMS_lat($metaInfo{ULLat});
    $metaInfo{ULLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{ULLong});
    $metaInfo{ULLong} = $tmp;
    $metadata{"Upper Left Corner"} = $metaInfo{ULLat}.", ".$metaInfo{ULLong};

    # format URLat coordinate values
    $tmp = format_DMS_lat($metaInfo{URLat});
    $metaInfo{URLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{URLong});
    $metaInfo{URLong} = $tmp;
    $metadata{"Upper Right Corner"} = $metaInfo{URLat}.", ".$metaInfo{URLong};

    # format LRLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LRLat});
    $metaInfo{LRLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LRLong});
    $metaInfo{LRLong} = $tmp;
    $metadata{"Lower Right Corner"} = $metaInfo{LRLat}.", ".$metaInfo{LRLong};

    # format LLLong coordinate values
    $tmp = format_DMS_lat($metaInfo{LLLat});
    $metaInfo{LLLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{LLLong});
    $metaInfo{LLLong} = $tmp;
    $metadata{"Lower Left Corner"} = $metaInfo{LLLat}.", ".$metaInfo{LLLong};

    # format Day or Night value
    $metadata{"Day or Night"} = Day_Night($metaInfo{DayNight});

    # include the cloud cover value if one is present
    if (defined $metaInfo{SceneCc})
    {
        $metadata{"Cloud Cover"} = $metaInfo{SceneCc} . "%";
        push (@order, "Cloud Cover");
    }

    # include the browse type value if one is present
    if (defined $metaInfo{BrowseType})
    {
        $metadata{"Browse Type"} = $metaInfo{BrowseType};
        push (@order, "Browse Type");
    }

    # name of the glossary pages used for MODIS
    my $page = "glossary_modis.html#";
 
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
    my (@msgs) = @_; # for debugging only, do not print messages in prod
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
    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('ModisFGDC.cgi?secondary_id=$secondaryid&dataset=$dataset&acq_date=$acqDate&LLLong=$LLLong&URLong=$URLong&ULLat=$ULLat&LRLat=$LRLat');\">";
    print "FGDC Metadata</a></p>";
 }

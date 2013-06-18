#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the
# scene id, grid column and grid row passed in

use strict;
use CGI qw(:standard escapeHTML);
use File::Find;

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $taintedsecondaryid = param('secondary_id');
my $sceneid;
my $gridcol;
my $gridrow;
my $secondaryid;

# unencode the scene id
$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip down the path for taint mode.  Note the perl library needs /bin for
# using pwd.
$ENV{'PATH'} = '/bin';

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
if ($taintedsceneid =~ /^(\w{7}\d{9})$/)
{
    $sceneid = $1;
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal"); # exits
}

# untaint the gridcol and gridrow
if ($taintedpath =~/^(\d{4})$/)
{
    $gridcol = $1;
}
else
{
    handleerror("Path $taintedpath is not legal"); # exits
}
if ($taintedrow =~/^(\d{4})$/)
{
    $gridrow = $1;
}
else
{
    handleerror("Row $taintedrow is not legal"); # exits
}
if ($taintedsecondaryid =~ /^(\d{3,9})$/)
{
    $secondaryid = $1;
}
else
{
    handleerror("Scene ID $taintedsecondaryid is not legal"); # exits
}

# build the directory name for this sensor/gridcol/gridrow/year
my $dirname = "nhap/";
if (substr($sceneid, 1, 1) eq "B")
{
    $dirname .= "bw/";
}
else
{
    $dirname .= "cir/";
}
$dirname .= $gridcol . "/" . $gridrow;

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

    winHeight = 760;
    winWidth = 480;

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
    win.location = "glossary_nhap.html#" + anchor;
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
buildMetaTable($metafile,$gridcol,$gridrow);
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

# routine to format the film type
sub formatFilmType
{
    my $type = $_[0];
    return "Black and White" if ($type eq "BW");
    return "Color Infrared" if ($type eq "CIR");
    return $type;
}

sub buildMetaTable
{
    my ($filename,$gridcol,$gridrow) = @_;

    my %metaInfo;   # A hash holding the metadata names;
    my %metadata;   # A hash holding the metadata names; 

    # This is the order we want the items to appear in the table
    my @order = (
                "Project Name",
                "Roll Number",
                "Frame Number",
                "Acquisition Date",
                "Film Type",
                "Scale",
                "Upper Left Corner",
                "Upper Right Corner",
                "Lower Left Corner",
                "Lower Right Corner",
                "Scene Center",
                "County",
                "State",
                "Nominal Focal Length",
                "Entity Id",
                "Product Available"
                );
    
   my %glossaryLinks = (
                        "Project Name" => "project_name",
                        "Roll Number" => "roll_number",
                        "Frame Number" => "frame_number",
                        "Acquisition Date" => "acquisition_date",
                        "Film Type" => "film_type",
                        "Scale" => "scale",
                        "Station" => "station",
                        "Upper Left Corner" => "ul_corner_lat",
                        "Upper Right Corner" => "ur_corner_lat",
                        "Lower Left Corner" => "ll_corner_lat",
                        "Lower Right Corner" => "lr_corner_lat",
                        "Scene Center" => "scene_center_lat",
                        "County" => "county",
                        "State" => "state",
                        "Nominal Focal Length" => "nominal_focal_length",
                        "Project Number" => "project_number",
                        "Entity Id" => "entity_id",
                        "Product Available" => "product_available"
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

    $metadata{"Project Name"} = $metaInfo{Project};
    $metadata{"Roll Number"} = $metaInfo{Roll};
    $metadata{"Frame Number"} = $metaInfo{Frame};
   
    # format acquisition date
    $metadata{"Acquisition Date"} = substr($metaInfo{AcqDate},0,4).'/'.
               substr($metaInfo{AcqDate},4,2)."/".
               substr($metaInfo{AcqDate},6,2);
    $metadata{"Film Type"} = formatFilmType($metaInfo{FilmType});
    $metadata{"Scale"} = $metaInfo{Scale};
    $metadata{"Station"} = $metaInfo{Station};

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

    # format the Scene Center coordinate values
    $tmp = format_DMS_lat($metaInfo{SCLat});
    $metaInfo{SCLat} = $tmp;
    $tmp = format_DMS_lon($metaInfo{SCLong});
    $metaInfo{SCLong} = $tmp;
    $metadata{"Scene Center"} = $metaInfo{SCLat}.", ".$metaInfo{SCLong};
    
    $metadata{"County"} = $metaInfo{County};
    $metadata{"State"} = $metaInfo{State};
    $metadata{"Nominal Focal Length"} = $metaInfo{FocalLength};
    $metadata{"Entity Id"} = $metaInfo{entityID};

    if ($metaInfo{SiloPath} !~ /^\s*$/)
    {
        $metadata{"Product Available"} = "Medium Resolution and Scanned ".
                                         "Products";
    }
    else
    {
        $metadata{"Product Available"} = "Scanned only Products";
    }

    
    my $page = "glossary_nhap.html#";

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

        print "<tr><td><a href=\"#\" ";
        print "onClick=\"loadmeta('$glossaryLinks{$metaItem}','$page');\">";
        print "$metaItem</a></td>";
        print "<td>$metadata{$metaItem}</td></tr>\n";
    }
    print "\n</TABLE>\n";
}

# routine to add link to FGDC metadata
sub addFgdcMetadata
{
    my $collection_id = '3092';
    my $id = substr($sceneid,7);
    print "<p><p align=center><a href=\"#\" ";
    print "onClick=\"loadfgdcmeta('http://earthexplorer.usgs.gov/form/fgdcmetadatalookup?collection_id=$collection_id&entity_id=$secondaryid&primary_key=$secondaryid&pageView=1');\">";
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


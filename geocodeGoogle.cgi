#!/usr/bin/perl -Tw

# This cgi script identifies possible locations for the
# user-supplied address, using the Google Geocoding API
# and returns up up to 25 results as a text string in the form:
# Found:lon:lat:label::lon:lat:label
# Or
# Error:message
# Or
# NotFound: No matches found

use strict;
use CGI qw(:standard);

# include the module that includes our client, channel, and cryptographic key
use lib "./.security";
use geocodeGoogle qw(geocode);

# get the address from the URL query string
my $tainted_address = param('address');
my $response = 'Error: internal error'; # return value, override below

if ($tainted_address)
{
    my $address = utf8_encode($tainted_address);

    $response = geocode($address);
}
else
{
    $response = 'Error: no address to search';
}

# print output to be picked up by the caller
print "Content-type: text/plain\n\n";
print "$response\n";

exit 0;

#############################################################################
# URL-encode a string as UTF-8
# which also serves to un-taint the input
#############################################################################
sub utf8_encode
{
    my $str = $_[0];
    # replace all space with +
    $str =~ s/ /\+/g;
    # leave alone A-Z a-z 0-9 _ + . - * and any existing hex %xy
    # everything else gets converted to its 2-digit hex representation as %xy
    $str =~ s/([^\w\+\.\-\*(%\x7f{2})])/"%" . uc(sprintf("%2.2x",ord($1)))/eg;

    return $str;
}


<?php

$link = mysql_connect('140.138.144.161','root','h2z5DhFn');
if (!$link)
  {
  die('Could not connect: ' . mysql_error());
  }

$CarStatus = $_POST['car_state'];
$Valid = $_POST['location_state'];
$lat = $_POST['location_latitude'];
$lon = $_POST['location_longitude'];
$Time = $_POST['car_save_time'];

echo $Time;

mysql_select_db('driver_management', $link);

if (!mysql_query("INSERT INTO taxi_save_data (CarStatus, Valid, lat, lon, Time) values ('$CarStatus', '$Valid', '$lat', '$lon', '$Time')", $link))
  {
  die('Error: ' . mysql_error());
  }

echo '1 record added';
mysql_close($link);
?>
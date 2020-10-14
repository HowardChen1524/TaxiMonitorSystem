<?php
$link = mysql_connect('140.138.144.161','root','h2z5DhFn');
if (!$link)
  {
  die('Could not connect: ' . mysql_error());
  }

mysql_select_db('driver_management', $link);

$car_type_all = mysql_query("SELECT * FROM taxi_car_type");

while($car_type = mysql_fetch_array($car_type_all)){
    echo $car_type['car_type']  . "\n";
    echo $car_type['car_door_id'] . "\n";
    echo $car_type['car_door_closed'] . "\n";
}

mysql_close($link);
?>
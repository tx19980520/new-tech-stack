<?php
$con = mysqli_connect("192.168.99.100:3306","root","12345");
if (!$con)
  {
  die('Could not connect: ');
  }

if (mysqli_query($con, "CREATE DATABASE Temage"))
  {
  echo "Database created";
  }
else
  {
  echo "Error creating database: ";
  }

mysqli_close($con);
?>

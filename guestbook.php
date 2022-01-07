<?php
session_start();
$post_data=escapeshellarg(json_encode($_POST));
$query_params=escapeshellarg(json_encode($_GET));
$sess_id=session_id();
passthru("POST_DATA=$post_data QUERY_PARAMS=$query_params SESSION_ID=$sess_id ./bb guestbook.clj");
?>

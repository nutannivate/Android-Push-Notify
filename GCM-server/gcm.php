<?php
  //generic php function to send GCM push notification
   function sendPushNotificationToGCM($registatoin_ids, $message) {
    	//Google cloud messaging GCM-API url
        $url = 'https://android.googleapis.com/gcm/send';
        
	$fields = array(
            'registration_ids' => $registatoin_ids,
            'data' => $message,
        );
    	
			// Google Cloud Messaging GCM API Key
    	define("GOOGLE_API_KEY", "YOUR-API-KEY");   
        $headers = array(
            'Authorization: key=' . GOOGLE_API_KEY,
            'Content-Type: application/json'
        );
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    		curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
        $result = curl_exec($ch);      
        if ($result === FALSE) {
            die('Curl failed: ' . curl_error($ch));
        }
        curl_close($ch);
        return $result;
    }
?>
<?php
   
  //this block is to post message to GCM on-click
  $pushStatus = "";
  if(!empty($_GET["push"])) {
	//define the path and name of the xml file
	$xml_file = 'GCMRegId.xml';
	//load the content of the XML file and create a new XML object
	$XML_file_content = simplexml_load_file($xml_file);
	//get All Device ids from xml object to Which Message has to be Sent 
	$gcmRegIds = $XML_file_content->REG_ID;
	//create new array and add all REG_IDs to array
	$gcmRegIds_array = array();    
	foreach ($gcmRegIds as $item) {
		array_push($gcmRegIds_array, (String) $item);
	}
	//save message string in variable 
    	$pushMessage = $_POST["message"];
    
	//if both REG_ID and message not null, pass to the function 'sendPushNotificationToGCM'
    	if (isset($gcmRegIds_array) && isset($pushMessage)) {   
        	$message = array("message" => $pushMessage);
      		$pushStatus = sendPushNotificationToGCM($gcmRegIds_array, $message);
    	}  
  }
   
  //this block is to receive the GCM regId from external (mobile apps)
  if(!empty($_GET["shareRegId"])) {
	$gcmRegID  = $_POST["regId"];
	//check if xml file exist or not	
	if(file_exists('GCMRegId.xml')){
		//get all REG IDs from xml file
		$XML_file_content = simplexml_load_file('GCMRegId.xml');
        	$gcmRegIds = $XML_file_content->REG_ID;
		$gcmRegIds_array = array();
        	
		//store REG_IDs in array
		foreach ($gcmRegIds as $item) {
                	array_push($gcmRegIds_array, (String) $item);
        	}

		//add REG ID to xml file if it is not exist in array
    		if (!in_array((String) $gcmRegID, $gcmRegIds_array))
    		{
			$doc = new SimpleXMLElement("GCMRegId.xml", null, true);
       			$doc->addChild('REG_ID',$gcmRegID);
               		$doc->asXML('GCMRegId.xml');
               		file_put_contents('GCMRegId.xml', $doc->asXML());
		}
	} else {
		//create new xml file and add REG_ID
    		$doc = new DOMDocument( "1.0" );
		//create root tag
    		$root = $doc->createElement( 'GCM_REG_IDs' );
        	$doc->appendChild( $root );
		//create child tag
		$ele = $doc->createElement("REG_ID");
		//save the value to the child tag
		$ele->nodeValue = $gcmRegID;
		$root->appendChild( $ele );
		//save xml file
        	$doc->save('GCMRegId.xml');
	}
    	echo "Ok!";
    	exit;
  }
?>
<html>
    <head>
        <title>Google Cloud Messaging (GCM) Server in PHP</title>
    </head>
  <body>
    <h1>Google Cloud Messaging (GCM) Server in PHP</h1>
    <form method="post" action="gcm.php/?push=1">                                     
      <div>                               
        <textarea rows="2" name="message" cols="23" placeholder="Message to transmit via GCM"></textarea>
      </div>
      <div><input type="submit"  value="Send Push Notification via GCM" /></div>
    </form>
    <p><h3><?php echo $pushStatus; ?></h3></p>       
    </body>
</html>

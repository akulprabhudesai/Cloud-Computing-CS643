import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.TextDetection;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.File;

public class TextRecognition {

	public static BasicSessionCredentials SessionCredentials() throws Exception {
		String access_id, secret_key, session_token;
		String str = new File(ImageRecognition.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				.getParent();
		File file = new File(str + "/credentials");
		List<String> lines = Collections.emptyList();
		lines = Files.readAllLines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8);
		access_id = lines.get(1).split("=")[1];
		secret_key = lines.get(2).split("=")[1];
		session_token = lines.get(3).split("=")[1];
		BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(access_id, secret_key, session_token);
		return sessionCredentials;
	}

	public static void main(String[] args) throws Exception {

		String bucket = "njit-cs-643";
		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(SessionCredentials())).withRegion("us-east-1")
				.build();
		final AmazonSQS sqs = AmazonSQSClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(SessionCredentials())).withRegion("us-east-1")
				.build();
        		final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion("us-east-1").build();
		ListObjectsV2Result resultObjects = s3.listObjectsV2(bucket);
		List<S3ObjectSummary> ObjectsList = resultObjects.getObjectSummaries();
        		final ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				"https://sqs.us-east-1.amazonaws.com/435513664146/akul.fifo");
		receiveMessageRequest.setMaxNumberOfMessages(10);
		FileWriter FileWrite = new FileWriter("Result.txt", false);
		while (true) {
			final List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			for (final Message message : messages) {
				if (message.getBody().contains("-1") == true) {
                    final String messageReceiptHandle = message.getReceiptHandle();
					sqs.deleteMessage(new DeleteMessageRequest(
							"https://sqs.us-east-1.amazonaws.com/435513664146/akul.fifo", messageReceiptHandle));
					FileWrite.close();
					System.exit(0);
				}
				for (S3ObjectSummary current : ObjectsList) {

					if (current.getKey().contains(message.getBody())) {
						System.out.println("Receieved Image: " + current.getKey());
						FileWrite.write(message.getBody());
						FileWrite.write("\t");
						DetectTextRequest request = new DetectTextRequest().withImage(
								new Image().withS3Object(new S3Object().withName(current.getKey()).withBucket(bucket)));
						try {
							DetectTextResult textresult = rekognitionClient.detectText(request);
							List<TextDetection> textDetections = textresult.getTextDetections();
							for (TextDetection text : textDetections) {
								if (text.getConfidence() >= 90 && text.getId() == 0) {
									System.out.println("Detected: " + text.getDetectedText());
									FileWrite.write(text.getDetectedText());
									System.out.println();
								}
							}
						} catch (AmazonRekognitionException e) {
							e.printStackTrace();
						}
						final String messageReceiptHandle = message.getReceiptHandle();
						sqs.deleteMessage(new DeleteMessageRequest(
								"https://sqs.us-east-1.amazonaws.com/435513664146/akul.fifo", messageReceiptHandle));
                        FileWrite.write("\n");
					}
				}
				System.out.println();
			}
		}
	}
}

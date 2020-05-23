
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/** 
 * This code expects that you have AWS credentials set up per:
 * http://docs.aws.amazon.com/java-sdk/latest/developer-guide/setup-credentials.html
 */

public class ImageRecognition {

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
		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(SessionCredentials())).withRegion("us-east-1")
				.build();
		final AmazonSQS sqs = AmazonSQSClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(SessionCredentials())).withRegion("us-east-1")
				.build();
		final String QUE = "https://sqs.us-east-1.amazonaws.com/435513664146/akul.fifo";

		final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion("us-east-1").build();
		ListObjectsV2Result result = s3.listObjectsV2("njit-cs-643");
		List<S3ObjectSummary> objects = result.getObjectSummaries();
		for (S3ObjectSummary current : objects) {
			DetectLabelsRequest request = new DetectLabelsRequest()
					.withImage(new Image().withS3Object(new S3Object().withName(current.getKey()).withBucket("njit-cs-643")))
					.withMaxLabels(10).withMinConfidence(75F);

			try {
				DetectLabelsResult LabelResult = rekognitionClient.detectLabels(request);
				List<Label> labels = LabelResult.getLabels();
				final Map<String, String> values = new HashMap<>();
				values.put("akul.fifo", "true");
				values.put("ContentBasedDeduplication", "true");
				for (Label CurrentLabel : labels) {
					if (CurrentLabel.getName().equals("Car") && CurrentLabel.getConfidence() >= 90) {
						System.out.println("Car detected: " + current.getKey());
						final com.amazonaws.services.sqs.model.SendMessageRequest sendMessageRequest = new com.amazonaws.services.sqs.model.SendMessageRequest(
								QUE, current.getKey());
						sendMessageRequest.setMessageGroupId("messageGroup1");
						sendMessageRequest.setMessageDeduplicationId(current.getETag());
						final com.amazonaws.services.sqs.model.SendMessageResult sendMessageResult = sqs
								.sendMessage(sendMessageRequest);
					}
				}
			} catch (AmazonRekognitionException e) {
				e.printStackTrace();
			} catch (final AmazonServiceException ase) {
				ase.printStackTrace();
			} catch (final AmazonClientException ace) {
				ace.printStackTrace();
			}
		}
		final com.amazonaws.services.sqs.model.SendMessageRequest sendMessageRequest = new com.amazonaws.services.sqs.model.SendMessageRequest(
				QUE, "-1");
		sendMessageRequest.setMessageGroupId("messageGroup1");
		sendMessageRequest.setMessageDeduplicationId("-1");
		final com.amazonaws.services.sqs.model.SendMessageResult sendMessageResult = sqs
				.sendMessage(sendMessageRequest);
	}
}
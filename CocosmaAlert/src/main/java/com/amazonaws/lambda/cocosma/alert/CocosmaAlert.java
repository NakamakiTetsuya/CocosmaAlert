package com.amazonaws.lambda.cocosma.alert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;

public class CocosmaAlert implements RequestHandler<String, String> {

	@Override
	public String handleRequest(String input, Context context) {
		final String BASE_URL = "http://matsumoto.fudousan.co.jp";
		final String SEARCH_URL = "/sell/list/3/";
		final String LINE_SEP = System.getProperty("line.separator");
		final String SNS_ARN = System.getenv("COCOSMA_ALERT_ARN");
		final String REGION = "us-west-2";
		final String SUBJECT = "ココスマ更新情報";
		Document document = null;

		try {
			document = Jsoup.connect(BASE_URL + SEARCH_URL).data("list_type", "detail").data("page_disp", "100")
					.data("sort_type", "kakaku_amt").data("sort_vector", "asc").data("kakaku_l", "1000")
					.data("kakaku_h", "2500").data("matori_[8]", "3-LDK").data("matori_[9]", "4-K")
					.data("matori_[10]", "4-DK").data("matori_[11]", "4-LDK").data("matori_max", "以上")
					.data("search", "1").get();
		} catch (IOException e) {
			return null;
		}

		List<String> texts = new ArrayList<>();
		List<String> infos = new ArrayList<>();
		List<String> urls = new ArrayList<>();
		List<String> updates = new ArrayList<>();

		document.getElementsByTag("h3").forEach(e -> texts.add(e.text()));
		document.getElementsByTag("h4").forEach(e -> infos.add(e.text()));
		document.select("div .box2").forEach(e -> urls.add(BASE_URL + e.getElementsByTag("a").attr("href")));
		document.select("div .box3").forEach(e -> updates.add(e.getElementsByTag("time").attr("datetime")));

		StringBuffer sb = new StringBuffer();
		AtomicInteger i = new AtomicInteger();

		updates.forEach(s -> {
			if (DateTime.parse(s).isEqual(new DateTime().withTimeAtStartOfDay())) {
				sb.append("【" + texts.get(i.get()) + "】" + LINE_SEP);
				sb.append(infos.get(i.get()) + LINE_SEP);
				sb.append(urls.get(i.getAndIncrement()) + LINE_SEP + LINE_SEP);
			}
		});

		return sb.length() <= 1 ? null
				: AmazonSNSClientBuilder.standard().withRegion(REGION).build().publish(SNS_ARN, sb.toString(), SUBJECT)
						.getMessageId();
	}

}

/**
 * 
 */
package com.qmetry.qaf.automation.support.testai;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;
import static org.openqa.selenium.remote.CapabilityType.SUPPORTS_JAVASCRIPT;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.ws.rs.core.MediaType;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CommandPayload;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.Response;

import com.google.common.collect.ImmutableMap;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebElement;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebComponent;
import com.qmetry.qaf.automation.util.JSONUtil;
import com.qmetry.qaf.automation.util.StringUtil;
import com.qmetry.qaf.automation.ws.rest.DefaultRestClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * This is Test.ai enabled component uses following properties to configure test.ai.
 * <ul>
 * <li>testai.training.mode - boolean enable/disable trainning mode, default true. Disable after one or more execution in trainning mode to improve execution performance.
 * <li>testai.server.url - server url
 * <li>testai.api.key - test.ai api key
 * </ul>
 * 
 * 
 * @author chirag.jayswal
 *
 */
public class TestAiElement extends QAFWebComponent {
	private static final String runID = UUID.randomUUID().toString();

	private double multiplier;

	/**
	 * @param locator
	 */
	public TestAiElement(String locator) {
		super(locator);
	}

	/**
	 * @param driver
	 */
	public TestAiElement(QAFExtendedWebDriver driver) {
		super(driver);
	}

	/**
	 * @param driver
	 * @param by
	 */
	public TestAiElement(QAFExtendedWebDriver driver, By by) {
		super(driver, by);
	}

	/**
	 * @param parent
	 * @param locator
	 */
	public TestAiElement(QAFExtendedWebElement parent, String locator) {
		super(parent, locator);
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	protected void load() {
		try {
			if (null == id || (id == "-1")) {
				super.load();
				if (getBundle().getBoolean("testai.training.mode", true) && (boolean)getMetaData().getOrDefault("testai", true)) {
					cacheable = true;
					// update Classification
					String key = (String) classify(getDescription()).get("key");
					if (StringUtil.isNotBlank(key)) {
						updateElement(this, key, getDescription(),
								(boolean) getMetaData().getOrDefault("trainIfNecessary", true));
					}
				}
			}
		} catch (TimeoutException e) {
			// lookup
			Map<String, Object> classification = classify(getDescription());
			Map<String, Object> eleFromClassification = (Map<String, Object>) classification.get("elem");

			if (null != eleFromClassification) {
				cacheable = true;
				Dimension size = new Dimension(
						((Number) eleFromClassification.get("width")).intValue() / (int) multiplier,
						((Number) eleFromClassification.get("height")).intValue() / (int) multiplier);

				Point location = new Point(((Number) eleFromClassification.get("x")).intValue() / (int) multiplier,
						((Number) eleFromClassification.get("y")).intValue() / (int) multiplier);
				int cX = location.x + size.width / 2;
				int cY = location.y + size.height / 2;
				
				QAFExtendedWebElement eleByJs = new QAFExtendedWebElement(
						String.format("js=return document.elementFromPoint(%d, %d) || document.elementFromPoint(%d, %d);", cX, cY,location.x, location.y));

				if (isJavascriptEnabled() && eleByJs.isPresent()) {
					setId(eleByJs.getId());
				} else {
					setId("testai");

					getMetaData().put("text", eleFromClassification.get("text"));
					getMetaData().put("tagName", eleFromClassification.get("class"));

					// getMetaData().put("size", size);
					// getMetaData().put("location", location);

					getMetaData().put("x", location.x);
					getMetaData().put("y", location.y);
					getMetaData().put("width", size.width);
					getMetaData().put("height", size.height);
					// this.property = property //TODO: not referenced/implemented on python side??
					// getMetaData().put("rectangle", new Rectangle(location, size));
				}
			}
			if (null == id || (id == "-1")) {
				throw e;
			}
		}
	}

	boolean isJavascriptEnabled() {
		return ((null ==
		 getWrappedDriver().getCapabilities().getCapability(SUPPORTS_JAVASCRIPT))
		 || getWrappedDriver().getCapabilities().is(SUPPORTS_JAVASCRIPT));
	}

	private Map<String, Object> classify(String elementName) {
		String pageSource = "", msg = "test.ai driver exception";
		try {
			pageSource = getWrappedDriver().getPageSource();
		} catch (Throwable e) {

		}

		try {
			String screenshotBase64 = getWrappedDriver().getScreenshotAs(OutputType.BASE64);
			multiplier = 1.0 * ImageIO.read(OutputType.FILE.convertFromBase64Png(screenshotBase64)).getWidth()
					/ getWrappedDriver().manage().window().getSize().width;

			
			Map<String, String> formParams = ImmutableMap.of("api_key", getBundle().getString("testai.api.key"), "screenshot",
					screenshotBase64, "source", pageSource, "label", elementName, "runId", runID);
			
			MultivaluedMapImpl form =new MultivaluedMapImpl();
			for(Entry<String, String> kv : formParams.entrySet()) {form.putSingle(kv.getKey(),kv.getValue());}

			ClientResponse res = getClient().resource(getBundle().getString("testai.server.url")).path("classify")
					.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,form);

			String resString = res.getEntity(String.class);
			Map<String, Object> resMap = JSONUtil.toMap(resString);
			if ((Boolean) resMap.get("success")) {
				logger.info("Successfully classified: " + elementName);
				return resMap;// (Map<String, Object>) resMap.get("elem");
			}
			String rawMsg = (String) resMap.get("message");

			if (StringUtil.isNotBlank(rawMsg)) {
				String cFailedBase = "Classification failed for element_name: ";

				if (rawMsg.contains("Please label") || rawMsg.contains("Did not find"))
					msg = String.format("%s%s - Please visit %s/label/%s to classify", cFailedBase, elementName,
							getBundle().getString("testai.server.url"), elementName);
				else if (rawMsg.contains("frozen label"))
					msg = String.format(
							"%s%s - However this element is frozen, so no new screenshot was uploaded. Please unfreeze the element if you want to add this screenshot to training",
							cFailedBase, elementName);
				else
					msg = String.format("%s: Unknown error, here was the API response: %s", msg, resString);

				logger.error(msg);

			}
			return resMap;

		} catch (Throwable e) {
			e.printStackTrace();
		}
		return ImmutableMap.of();
	}

	private void updateElement(WebElement elem, String key, String elementName, boolean trainIfNecessary) {
		Rectangle rect = elem.getRect();
		
		String payload = String.format("{\"key\":\"%s\",\"api_key\":\"%s\",\"run_id\":\"%s\","
				+ "\"x\":%s,\"y\":%s,\"width\":%s, \"height\":%s,"
				+ "\"multiplier\":%s,\"train_if_necessary\":%s,\"label\":\"%s\"}",
				key, getBundle().getString("testai.api.key"), runID,
				rect.x, rect.y, rect.width, rect.height,
				multiplier, trainIfNecessary, elementName);
		
		
		ClientResponse res = getClient().resource(getBundle().getString("testai.server.url")).path("add_action").type(MediaType.APPLICATION_JSON).post(ClientResponse.class,payload);     
		if(res.getStatus()==200) {
			logger.info("Successfully updated: " + elementName);
		}else {
			logger.error("Unable to  update: " + elementName + " - " +res.getEntity(String.class));
		}
	}

	@Override
	protected Response execute(CommandPayload payload) {
		if (getId().equalsIgnoreCase("testai")) {
			Response response = new Response();
			switch (payload.getName()) {
			case DriverCommand.GET_ELEMENT_TEXT:
				response.setValue(getMetaData().get("text"));
				break;
			case DriverCommand.GET_ELEMENT_TAG_NAME:
				response.setValue(getMetaData().get("tagName"));
				break;
			case DriverCommand.GET_ELEMENT_LOCATION:
			case DriverCommand.GET_ELEMENT_LOCATION_ONCE_SCROLLED_INTO_VIEW:
			case DriverCommand.GET_ELEMENT_SIZE:
			case DriverCommand.GET_ELEMENT_RECT:
				response.setValue(getMetaData());
				break;
			case DriverCommand.CLICK:
			case DriverCommand.CLICK_ELEMENT:
				Point loc = getLocation();
				new Actions(getWrappedDriver()).moveByOffset(loc.getX(), loc.getY()).click().perform();
				break;
			case DriverCommand.SEND_KEYS_TO_ELEMENT:
				Point loc2 = getLocation();
				new Actions(getWrappedDriver()).moveByOffset(loc2.getX(), loc2.getY()).click()
						.sendKeys((String) payload.getParameters().get("text")).perform();
				break;
			case DriverCommand.SUBMIT_ELEMENT:
				sendKeys("\n");
				break;
			default:
				throw new UnsupportedOperationException();

			}
			return response;
		}
		return super.execute(payload);
	}

	private Client getClient() {
		Client c = (Client) getBundle().getObject("_testai.client");
		if (null == c) {
			c = new DefaultRestClient().getClient();
			c.removeAllFilters();
			getBundle().setProperty("_testai.client", c);
		}
		return c;
	}
}

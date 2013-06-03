/**
 * 
 */
package de.jetwick.snacktory;

/**
 * @author vikasing
 *
 */
public class MainTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HtmlFetcher fetcher = new HtmlFetcher();
		// set cache. e.g. take the map implementation from google collections:
		// fetcher.setCache(new MapMaker().concurrencyLevel(20).maximumSize(count).
		//    expireAfterWrite(minutes, TimeUnit.MINUTES).makeMap();

		JResult res;
		try {
			res = fetcher.fetchAndExtract("http://joelhooks.com/blog/2013/05/22/lessons-learned-kicking-off-an-angularjs-project/", 20000, false);
			String text = res.getText(); 
			String title = res.getTitle(); 
			String imageUrl = res.getImageUrl();
			System.out.println(title +"\n"+ text +"\n"+imageUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

/**
 * Copyright 2011 Pascal Christoph. The program is distributed under the terms of
 * the GNU General Public License, see https://www.gnu.org/licenses/gpl-3.0.html
 *
 * @date 2012-05-04
 */

package enrichment;

import org.junit.Test;

public class DisambiguateTest {

	@Test
	public void test() {
		String[] args={"./src/main/resources/enrichment/smallTest.nt"};//"lobid_dbpedia_bookTitle_links_ONE_wW.nt ; part-m-00805.nt
			Disambiguate.main(args);
	}

}

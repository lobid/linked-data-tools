/**
 * Copyright 2011 Pascal Christoph. The program is distributed under the terms of
 * the GNU General Public License, see https://www.gnu.org/licenses/gpl-3.0.html
 *
 * @date 2012-05-04
 */

package enrichment;

import java.io.File;
import java.io.FilenameFilter;

public class OnlyExtFilenameFilter implements FilenameFilter {
	String ext;

	public OnlyExtFilenameFilter(String ext) {
		this.ext = "." + ext;
	}

	public boolean accept(File dir, String name) {
		return name.endsWith(ext);
	}
}
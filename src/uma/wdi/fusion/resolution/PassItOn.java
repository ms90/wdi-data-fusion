/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uma.wdi.fusion.resolution;

import java.util.Set;
import org.w3c.dom.Node;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.utils.Pair;
import uma.wdi.fusion.utils.XMLUtils;

/* Conflict resolution function that leaves all values as they are
 * 
 * @author Volha
 * */
public class PassItOn extends AbstractResolutionFunction 
{
	// Does not make any changes to values
	@Override
	public Set<Pair> applyStrategy(Dataset ds, Node node, String path)
	{
		Set<Pair> result = getValueProvenancePairs(node, path);
		return result;
	}
}

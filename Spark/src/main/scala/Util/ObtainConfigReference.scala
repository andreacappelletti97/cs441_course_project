/*
 *
 *  Copyright (c) 2021. Mark Grechanik and Lone Star Consulting, Inc. All rights reserved.
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under
 *   the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *   either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 *
 */
package Util
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.{Failure, Success, Try}

class ObtainConfigReference

object ObtainConfigReference {
  private val config = ConfigFactory.load()
  private val logger = CreateLogger(classOf[ObtainConfigReference])
  private def ValidateConfig(confEntry: String):Boolean = Try(config.getConfig(confEntry)) match {
    case Failure(exception) => logger.error(s"Failed to retrieve config entry $confEntry for reason $exception"); false
    case Success(_) => true
  }

  def apply(confEntry:String): Option[Config] =
    if (ValidateConfig(confEntry)) {
      Some(config)
    } else {
      None
    }
}


import com.android.build.api.dsl.ApplicationProductFlavor
import org.gradle.api.NamedDomainObjectContainer

/**
 * Private build flavors configuration
 * This file should only exist in the private repository
 */
fun NamedDomainObjectContainer<out ApplicationProductFlavor>.configurePrivateFlavors() {
    create("noble") {
        dimension = "env"
        applicationId = "me.proton.lumo"
        buildConfigField("String", "BASE_DOMAIN", "\"cavendish.proton.black\"")
        buildConfigField("String", "OFFER_ID", "\"bf-test\"")
    }
}

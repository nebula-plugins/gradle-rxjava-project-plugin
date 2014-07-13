package nebula.plugin.rxjavaproject

import spock.lang.Specification

/**
 * Created by jryan on 7/10/14.
 */
class RxJavaPublishingPluginSpec extends Specification {
    def 'testCalculateUrlFromOrigin'(String origin, String uri) {
        expect:
        RxJavaPublishingPlugin.calculateUrlFromOrigin(origin) == uri

        where:
        origin                                           | uri
        'git@github.com:reactivex/rxjava-core.git'       | 'https://github.com/reactivex/rxjava-core'
        'ssh://git@github.com:reactivex/rxjava-core.git' | 'https://github.com/reactivex/rxjava-core'
        'https://github.com:reactivex/rxjava-core.git'   | 'https://github.com/reactivex/rxjava-core'

    }
}

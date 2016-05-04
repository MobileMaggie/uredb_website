
import java.util.logging.Logger;

import org.ac.uk.reading.ure.uredb.*;

import grails.converters.*;
import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import org.springframework.context.ApplicationContext


/**
 * UreTagLib
 *
 * on caching in taglibs, note: https://jira.grails.org/browse/GPCACHE-17
 * 
 */
class UreTagLib {
    static namespace = "ure"
    def  springSecurityService;
    def recordService;
    def ApiController;
    def listRecords  =  { attrs,body->
        def var = attrs.var?:"t";
        Uremeta.list().each {

            it.getClass().fields.each {n->  println n; }
            out << body((var):it)
        }
    }

    def currentUser = {

        def user = springSecurityService.getCurrentUser();

        user =  springSecurityService.getPrincipal();
        if (user.getClass().getName() != 'java.lang.String') {

            def user2 =     org.ac.uk.reading.ure.sec.User.findByUsername(user.username);
            def json = user2 as JSON;
            out << json;
        }
        else {
            out << "anonymousUser"
        }
    }
    //TODO these should be provided by a service
    def getRecordById  =  { attrs,body->
        // if we find it, return a rendered version

        // if we don't, return a not found
        log.info attrs.id;
        def acc = "13.10.28"

        def rec =  Uremeta.get(attrs.id);

        rec.properties.sort().each {k,v->


            out  << body(("t"):['key':k,'value':v])

        }

    }
    def getRecordProperty  = { attrs,body->
        // if we find it, return a rendered version

        // if we don't, return a not found

        def prop = attrs.prop;
        def val = attrs.val;

        //              def rec =  Uremeta.get(attrs.id);
        def rec =  Uremeta.findAll('from Uremeta as u where u.'+prop+' like ? ', [val]);
        //TODO should be an error if more than one
        rec.each {r->
            r.properties.sort().each {k,v->


                out  << body(("t"):['key':k,'value':v])

            }
        }
    }

    def getRecordByAccNum  = { attrs,body->
        // if we find it, return a rendered version

        // if we don't, return a not found
        log.info attrs.id;
        def acc = attrs.acc;

        //              def rec =  Uremeta.get(attrs.id);
        def rec =  Uremeta.findAll('from Uremeta as u where u.accession_number like ? ', [acc]);
        rec.each {r->
            r.properties.sort().each {k,v->


                out  << body(("t"):['key':k,'value':v])

            }
        }
    }
    // print warning if:
    // the record id from this session is the same as the record id from application c
    def isSimulEdit = { attr, body ->
        //        if (servletContext.currentEditPage == servletContext.currentEditPage) {
        //        out << body() << servletContext.currentEditPage;
        //        }
        // if (servletContext.currentEditor != null && servletContext.currentEditPage==pageScope.getVariable('uremetaInstance').id ) {
        if (servletContext.currentEditor != null
        && servletContext.currentEditPage==pageScope.uremetaInstance.id?:0 ) {
            out << body();
        }
        else {
            // user can edit, so set edit flags
            //            servletContext.currentEditor=pageScope.currentUser;
            //            servletContext.currentEditPage=pageScope.uremetaInstance.id;

            out << 0;

        }


    }

    def bla1 = {attrs, body ->
        out << "hi"
    }
    def searchWidget = {attrs, body ->

        def m =attrs
        def klass  = m.klass
        def  res = m.results


        // in here do all the munging
        //
        out << render(template:"/taglibTemplates/searchWidget", model:m,)

    }
    /** 
     * attrs.pics -- A list of picture urls.
     * attrs.klass -- the css class to apply to the container
     */

    def gridWidgetForAccessionNumbers = {attrs, body ->


        def klass  = attrs.klass
        def  accs = attrs.accs
        def gridid = attrs.gridid;

        def model =[:];
        def width = (attrs.width) ? attrs.width : '248px'
        def height = (attrs.height) ? attrs.height : '248px'
        def info = [:]
        if (attrs.searchLimit) {
            if (accs.size() > attrs.searchLimit.toInteger()) {
                System.err.println attrs.searchLimit
                accs = accs[0..attrs.searchLimit.toInteger()];
            }
        }
        accs.eachWithIndex { acc,i ->
            def uri = i;
            def media;
            def meta;
            def rec = Uremeta.findByAccession_number(acc);
            meta = rec;
            if (rec.media[0]) {
                uri = rec.media[0].uri
                media = rec.media[0];

            }
            // there's no image assoc with this object
            else {

                media = [:]
                media.uri = ""
                media.uri_local = ""
                media.caption = ""
            }
            info[uri] = [:]
            info[uri]['media'] = media
            info[uri]['meta'] = meta
        }

        model['info'] =   info
        model['displayInfobox'] = (attrs.displayInfobox) ? attrs.displayInfobox : true;



        model['klass'] = klass;
        model['gridid'] = gridid;
        model['width'] = width;
        model['height'] = height;
        //log.warn model
        // in here do all the munging
        //
        out << render(template:"/taglibTemplates/gridWidget", model:model)

    }

    /**
     * attrs.pics -- A list of picture urls.
     * attrs.klass -- the css class to apply to the container
     */

    def gridWidgetForImageUris = {attrs, body ->


        def klass  = attrs.klass
        def uris = attrs.uris
        def gridid = attrs.gridid;

        def model =[:];
        def width = (attrs.width) ? attrs.width : '248px'
        def height = (attrs.height) ? attrs.height : '248px'


        model['info'] =   recordService.getImageInfos(uris)
        model['displayInfobox'] = (attrs.displayInfobox) ? attrs.displayInfobox : true;
        model['klass'] = klass;
        model['gridid'] = gridid;
        model['width'] = width;
        model['height'] = height;
        //log.warn model
        // in here do all the munging
        //
        out << render(template:"/taglibTemplates/gridWidget", model:model)

    }


    /**
     * attrs.pics -- A list of picture urls.
     * attrs.klass -- the css class to apply to the container
     */


    def gridWidgetForSearchResults = {attrs, body ->

        def model =[:];
        def klass  = (attrs.klass) ? attrs.klass : 'image-grid'
        def results = attrs.results;
        def gridid = (attrs.gridid) ? attrs.gridid : 'image-grid'
        def width = (attrs.width) ? attrs.width :    '248px'
        def height = (attrs.height) ? attrs.height : '248px'

        /**
         * convert to array of items of form 
         * [uri:[media: media, meta:rec]]
         *  item.media
         *  item.rec
         * 
         */

        def info =  [:]
        results.eachWithIndex { res,i->

            def uri = i;
            def media;
            def meta;

            if (res instanceof Uremeta) {
                meta = res;
                if (res.media[0]) {
                    uri = res.media[0].uri
                    media = res.media[0];

                }
                // there's no image assoc with this object
                else {
                    media = [:]
                    media.uri = ""
                    media.uri_local = ""
                    media.caption = ""
                }

            }
            // this is a media object
            // find the record for it.
            else {
                uri = res.uri;
                meta = Uremeta.findByAccession_number(res.resource_id);
                media = res
            }
            info[uri] = [:]
            info[uri]['media'] = media
            info[uri]['meta'] = meta
        }

        model['info'] =  info
        model['displayInfobox'] = (attrs.displayInfobox) ? attrs.displayInfobox.toBoolean() : true;
        model['klass'] = klass;
        model['gridid'] = gridid;
        model['width'] = width;
        model['height'] = height;

        out << render(template:"/taglibTemplates/gridWidget", model:model)

    }

    // This should be pre-computed and-or cached

    def europeanaWidget = {attrs, body ->


        def klass  = attrs.klass
        def uris = attrs.uris
        def gridid = (attrs.gridid) ? attrs.gridid : 'image-grid'
        def width = (attrs.width) ? attrs.width : '248px'
        def height = (attrs.height) ? attrs.height : '248px'
        def keywords = attrs.keywords
        def period = attrs.period
        def api_key = grailsApplication.config.europeana.wskey //  grails-app/conf/Private.groovy
        api_key = 'ZOPCEDTKBM'
        def search_url = "http://www.europeana.eu/api/v2/search.json?wskey=" + api_key

        def model =[:];

        // http://www.europeana.eu/api/v2/search.json?wskey=ZOPCEDTKBM&query='$QUERY'&start=10&thumbnail=true&rows=50'
        def query = ""
        if (keywords.size() > 1) {
            //  log.warn "kw = " + keywords
            query = keywords.join("+OR+");
        }
        else {
            query = keywords[0]
        }
        def startrec = 1
        def uri = search_url + "&query="+query+"&thumbnail=true&rows=100&profile=rich&start="+startrec

        model['info'] =   _getEuropeana(uri)

        model['more']  = []
        model['uri'] =[]+ uri
        def itemsCount = model['info']['itemsCount'] 
          
            2.times {
                // only iterate if there's more
                log.warn "items Count" + itemsCount;
                if (itemsCount > 99) {
                    startrec = 100 * (it + 1)
                    uri = search_url + "&query="+query+"&thumbnail=true&rows=100&profile=rich&start="+startrec
                    def moreItems = _getEuropeana(uri)
                    if (moreItems['info']) {
                        itemsCount = moreItems['info']['itemsCount'] 
                        model['more'] << moreItems
                        model['uri'] << uri
                    }
                    else {
                        itemsCount = 0;
                    }
                }
            }

        model['displayInfobox'] = (attrs.displayInfobox) ? attrs.displayInfobox : true;


        model['keywords'] = keywords;
        model['klass'] = klass;
        model['gridid'] = gridid;
        model['width'] = width;
        model['height'] = height;
        //log.warn model as JSON;
        // in here do all the munging
        out << render(template:"/taglibTemplates/europeanaWidget", model:model);

    }
/**
 * D3 barchart for word lists
 * 
 * @attrs words a map [word1:cnt,word2:cnt,...]
 * @attrs klass container class
 * @attrs max max number of bars to display
 */
    def wordmapWidget = { attrs,body ->
        def model = [:]
    
        def words = attrs.words;
       model['wordcount'] = words.sort{ a,b -> b.value <=> a.value}
       model['klass'] = attrs.klass
       model['max']  = attrs.max
       out << render(template:"/taglibTemplates/wordmapWidget", model:model);
        
    }
 /**
  * wordlist Widget
  * 
  * @attrs words array of words
  * @attrs f   the field
  * @attrs klass container klass
  * @attrs thumbs map of thumb for each word
  */
   def wordlistWidget = { attrs,body ->
      out << render(template:"/taglibTemplates/wordlistWidget",model:attrs);

    }
    
/**
 * dispatch europeana query. 
 * 
 * @param uri
 * @return
 */
    @Cacheable('pages')
    def _getEuropeana(uri) {
        def data = new URL(uri).getText()
        // log.warn data;
        def json =  JSON.parse(data);
        // also do the transform here.
     //   log.warn json;
    
        return json

    }

}

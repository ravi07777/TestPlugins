package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class OlamoviesProvider : MainAPI() {
    // This is the base domain. Cloudstream will auto-follow redirects.
    override var mainUrl = "https://olamovies.app"
    override var name = "Olamovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val homeItems = mutableListOf<HomePageList>()

        document.select(".sect > .row, .latest-posts").forEach { block ->
            val title = block.selectFirst("h3")?.text() ?: "Latest"
            val shows = block.select(".item, article").mapNotNull { element ->
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val showName = element.selectFirst(".title, h3")?.text() ?: ""
                
                newMovieSearchResponse(showName, href, TvType.Movie) {
                    this.posterUrl = poster
                }
            }
            if (shows.isNotEmpty()) homeItems.add(HomePageList(title, shows))
        }
        return HomePageResponse(homeItems)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select(".result-item, article").mapNotNull { element ->
            val title = element.selectFirst(".title, h3")?.text() ?: ""
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = element.selectFirst("img")?.attr("src")
            
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.title")?.text() ?: ""
        val poster = document.selectFirst(".poster img, .entry-content img")?.attr("src")
        val plot = document.selectFirst(".plot, .description")?.text()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        // Finds hidden video players on the Olamovies page
        document.select("iframe").forEach { iframe ->
            val link = iframe.attr("src")
            if (link.isNotBlank()) {
                loadExtractor(link, data, subtitleCallback, callback)
            }
        }
        return true
    }
}

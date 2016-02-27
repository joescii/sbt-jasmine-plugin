package com.joescii

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.HtmlPage
import net.sourceforge.htmlunit.corejs.javascript.{Function => JsFunction, ScriptableObject}

class JsRunner(val browserVersion:BrowserVersion) {
  private [this] val client = new WebClient(browserVersion)
  private [this] val options = client.getOptions()
  options.setHomePage(WebClient.URL_ABOUT_BLANK.toString())
  options.setJavaScriptEnabled(true)

//  client.getPage(this.getClass.getClassLoader.getResource("blank.html"))
  client.getPage("file://C:/code/sbt-jasmine-plugin/src/main/resources/blank.html")

  private [this] val window = client.getCurrentWindow().getTopWindow
  private [this] val page:HtmlPage = window.getEnclosedPage().asInstanceOf[HtmlPage] // asInstanceOf because ... java...

  def run(js:String):Unit = {
    val toRun = "function() {"+js+"\n};"
    val result = page.executeJavaScript(toRun)
    val func:JsFunction = result.getJavaScriptResult().asInstanceOf[JsFunction]

//    try {
      page.executeJavaScriptFunctionIfPossible(
        func,
        window.getScriptObject().asInstanceOf[ScriptableObject],
        Array.empty,
        page.getDocumentElement())
//    } catch {
//      case e:Exception =>
//    }
  }
}

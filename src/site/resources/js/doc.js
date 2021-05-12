var language = document.location.href.includes("_fr") ? "fr" : "en";


/** Table of content management **/
injectToc();

function injectToc(){
    if(!$("#bodyColumn").text().includes(getTitleToc())) {
        var divToc = document.createElement("div");
        divToc.id = "toc";
        var titre = document.createElement("h2");
        titre.innerText = getTitleToc();
        divToc.append(titre);
        var toc = populateUl("bodyColumn", 2);

        if(toc.children.length > 0){
            divToc.append(toc);
            $("#bodyColumn > h1").after(divToc);
        }
    }
}

function populateUl(parentSelector, titleLevel){
    var ul = document.createElement("ul");
    $("#" +parentSelector + " h" + titleLevel).each((i, elem)=> {
        var li = document.createElement("li");
        var link = document.createElement("a");
        link.href = "#" + $("#" +parentSelector + " h" + titleLevel + ":eq(" + i +") a")[0].name;
        link.text = elem.innerText;
        li.append(link);
        var sections = $("#" + parentSelector + " section:eq(" + i + ") section");
        if(sections.length > 0){
            li.append(populateUl(parentSelector + " section:eq(" + i + ") section", titleLevel+1));
        }
        ul.append(li);
    });

    return ul;
}

function getTitleToc(){
   var title;
    if (language === "fr"){
        title = "Table des matières";
    }
    else{
        title = "Table of content";
    }
    return title
}
if($(window).width() > 1200){
    $(".tab").hide();
    setActiveButton("pc");
}
else{
    $(".pc").hide();
    setActiveButton("tab");
}

$("a[href=#pc]").click(()=>{
    $(".tab").hide();
    $(".pc").show();
    setActiveButton("pc");
});

$("a[href=#tab]").click(()=>{
    $(".tab").show();
    $(".pc").hide();
    setActiveButton("tab");

})

$("a[href=#both]").click(()=>{
    $(".tab").show();
    $(".pc").show();
    setActiveButton("both");
})

function setActiveButton(btntarget){
    $("a").css("color", "#08c");
    $("a[href=#" + btntarget + "]").css("color", "purple");
}

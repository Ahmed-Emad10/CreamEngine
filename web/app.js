const express= require('express')
const mysql= require('mysql2')
const app = express()
const port = 3000
var stemmer = require('./porter-stemmer-master/porter').stemmer
var bodyParser = require('body-parser')
var urlencodedParser = bodyParser.urlencoded({ extended: false })
app.listen(port,()=>console.info(`Listening on port ${port}`))

//import {stemmer} from 'stemmer'              //sa
var natural = require('natural');             //ma

var myConnection = mysql.createConnection({
    host: "localhost",
    user: "root",
    password: "asd123asd",
    database: "searchengine",
    multipleStatements: true
})

myConnection.connect(function(err) {
    if (err) throw err;
    console.log("Connected!");
  })

var searchtopic;    //search value
var arrRes=[];      //to store query results
var results=[];


app.use(express.static('public'))
app.use('/css',express.static(__dirname + 'public/css'))
app.use('/js',express.static(__dirname + 'public/js'))

app.set('views', './views')
app.set('view engine', 'ejs')

app.get('',(req, res)=>{
    res.render('main')
})

app.post('/search', urlencodedParser,(req,res)=>{
    searchtopic=req.body.searchTopic;
    console.log(searchtopic)
    console.log(natural.PorterStemmer.stem('adidas'))

    console.log(stemmer('adidas'))
    

    var sql = ` SELECT doc_id,title,link,des 
                FROM Pages
                join Words 
                on doc_id=id
                WHERE word='${searchtopic}' ;`;

    myConnection.query(sql, async(error, arrRes, fields) => {
        //console.log(arrRes.length)
        // console.log(arrRes[1])

        if (error) res.send(error);
        if (!arrRes[0]) {
             res.json("Not Found");
        } 

        // for(var i=0 ; i<arrRes.length; i++){

        //     results.push({ doc_id:arrRes[i].doc_id, title:arrRes[i].title, link:arrRes[i].link, des:arrRes[i].des });
        //     // console.log("pushhhhhhhhhhhhhhhhhed")
        //     //console.log(arrRes[i].doc_id)

        //     //  console.log({
        //     //     doc_id:arrRes[i].doc_id,
        //     //     title:arrRes[i].title,
        //     //     link:arrRes[i].link,
        //     //     des:arrRes[i].des
        //     //  })
            
        // }
        // console.log(results)

        console.log(arrRes)
        var returned = arrRes.slice(0,10)
        res.render('Results',{ result:returned, page:"1" })
    })
 
})

app.post('/next', urlencodedParser,(req,res)=>{
    console.log(req.body)
    var returned = arrRes.slice((req.body.pageNum-1)*10,(req.body.pageNum-1)*10+10)
   // if(!arrRes[(req.body.pageNum-1)*10])
    res.render('Results',{ result:returned, page:req.body.pageNum })
})
app.post('/back', urlencodedParser,(req,res)=>{
    console.log(req.body)
    var returned = arrRes.slice((req.body.pageNum-1)*10,(req.body.pageNum-1)*10+10)
    res.render('Results',{ result:returned, page:req.body.pageNum })
})


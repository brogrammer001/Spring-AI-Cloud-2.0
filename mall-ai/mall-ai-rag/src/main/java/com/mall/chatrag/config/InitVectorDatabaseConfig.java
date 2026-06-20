package com.mall.chatrag.config;

import jakarta.annotation.PostConstruct;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;


@Configuration
public class InitVectorDatabaseConfig {

    //@Autowired
    //private VectorStore vectorStore;

    @Value("classpath:工时地址.md")
    private Resource opsFile;

    @PostConstruct
    public void init() {
        //1 读取文件
        /*TextReader textReader = new TextReader(opsFile);
        textReader.setCharset(Charset.defaultCharset());

        //2 文件转换为向量(开启分词)
        List<Document> list = new TokenTextSplitter().transform(textReader.read());
        //键不存在，首次插入,可以保存进向量数据库
        vectorStore.add(list);*/
    }

}

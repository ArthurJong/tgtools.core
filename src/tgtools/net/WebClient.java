package tgtools.net;

import tgtools.exceptions.APPErrorException;
import tgtools.util.StringUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by tian_ on 2016-06-25.
 */
public class WebClient implements IWebClient {
    private String m_Url;
    private String m_Encoding="UTF-8";
    private String m_Method="POST";
    private Map<String, String> m_Head;
    private boolean m_GZip;

    private List<String> m_Cookies;

    public WebClient() {
        m_Cookies= new ArrayList<String>();
        m_Head = new HashMap<String, String>();
        m_Head.put("accept", "*/*");
        m_Head.put("connection", "Keep-Alive");
        m_Head.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko");
        m_Head.put("Content-Type", "text/xml; charset=utf-8");
    }


    @Override
    public void setUrl(String p_Url) {
        m_Url = p_Url;
    }

    @Override
    public String getUrl() {
        return m_Url;
    }

    /**
     * 设置字符编码
     * @param p_Encoding
     */
    @Override
    public void setEncoding(String p_Encoding) {
        m_Encoding = p_Encoding;
    }

    /**
     * 获取字符编码
     */
    @Override
    public String getEncoding() {
        return m_Encoding;
    }

    /**
     * 设置HTTP 方法 如 ：GET,POST
     * @param p_Method
     */
    @Override
    public void setMethod(String p_Method) {
        m_Method = p_Method;
    }

    @Override
    public String getMethod() {
        return m_Method;
    }

    /**
     * 设置是否使用gzip 压缩（注：只适用POST）
     * @param p_GZip
     */
    @Override
    public void setGZip(boolean p_GZip) {
        m_GZip=p_GZip;
    }

    @Override
    public boolean getGZip() {
        return m_GZip;
    }

    /**
     * 增加head
     * @param p_Name
     * @param p_Value
     */
    @Override
    public void addHead(String p_Name, String p_Value) {
        m_Head.put(p_Name, p_Value);
    }

    @Override
    public Map<String, String> getHead() {
        return m_Head;
    }

    /**
     * 请求并返回收到的流
     * @param params 输入的参数 可以是任意字符 如常规 a=1&b=2 或json xml
     * @return
     * @throws APPErrorException
     */
    @Override
    public InputStream doInvokeAsStream(String params) throws APPErrorException {
        try {
            return doInvoke(params).getInputStream();
        } catch (IOException e) {
            throw new APPErrorException("获取返回信息出错",e);
        }
    }

    /**
     * 请求并返回收到的流
     * @param params 常规参数  params 会 拼接成 a=1&b=2 的形式
     * @return
     * @throws APPErrorException
     */
    @Override
    public InputStream doInvokeAsStream(Map<String, String> params) throws APPErrorException {
        return doInvokeAsStream(getParamsString(params));
    }

    /**
     * 请求并返回收到的字符
     * @param params 输入的参数 可以是任意字符 如常规 a=1&b=2 或json xml
     * @return
     * @throws APPErrorException
     */
    @Override
    public String doInvokeAsString(String params) throws APPErrorException {
        try {
            URLConnection conn=doInvoke(params);
            boolean isgzip=isGzip(conn.getHeaderFields());
            String encoding =getResponseEncoding(conn.getHeaderFields());
            if(isgzip)
            { return parseGZipString(conn.getInputStream(),m_Encoding);}
            return parseString(conn.getInputStream(),m_Encoding);
        } catch (IOException e) {
            throw new APPErrorException("获取返回信息出错",e);
        }
    }

    /**
     * 请求并返回收到的字符
     * @param params 常规参数  params 会 拼接成 a=1&b=2 的形式
     * @return
     * @throws APPErrorException
     */
    @Override
    public String doInvokeAsString(Map<String, String> params) throws APPErrorException {
        return doInvokeAsString(getParamsString(params));
    }

    /**
     * 请求并返回收到的字节
     * @param params 输入的参数 可以是任意字符 如常规 a=1&b=2 或json xml
     * @return
     * @throws APPErrorException
     */
    @Override
    public byte[] doInvokeAsByte(String params) throws APPErrorException {
        try {
            return parseByte(doInvoke(params).getInputStream());
        } catch (IOException e) {
            throw new APPErrorException("获取返回信息出错",e);
        }
    }

    /**
     * 请求并返回收到的字节
     * @param params 常规参数  params 会 拼接成 a=1&b=2 的形式
     * @return
     * @throws APPErrorException
     */
    @Override
    public byte[] doInvokeAsByte(Map<String, String> params) throws APPErrorException {
        return doInvokeAsByte(getParamsString(params));
    }

    /**
     * 将gzip信息解压
     * @param p_Stream 输入的gzip 数据
     * @param p_Encoding 字符编码
     * @return
     * @throws APPErrorException
     */
    private String parseGZipString(InputStream p_Stream,String p_Encoding) throws APPErrorException {
        GZIPInputStream gzin = null;
        try {
            gzin = new GZIPInputStream(p_Stream);

            InputStreamReader isr = new InputStreamReader(gzin, p_Encoding);
            BufferedReader br = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();
            String tempbf;
            while ((tempbf = br.readLine()) != null) {
                sb.append(tempbf);
                sb.append("\r\n");
            }
            isr.close();
            gzin.close();
            return sb.toString();



        } catch (IOException e) {
            throw new APPErrorException("GZIP数据解析错误",e);
        }
    }

    /**
     * 将输入的流转换成 字符
     * @param p_Stream 输入的流
     * @param p_Encoding 字符编码
     * @return
     * @throws APPErrorException
     */
    private String parseString(InputStream p_Stream,String p_Encoding) throws APPErrorException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            byte[] data = new byte[4096];
            int count = -1;
            while ((count = p_Stream.read(data, 0, 4096)) != -1)
                outStream.write(data, 0, count);

            return new String(outStream.toByteArray(), p_Encoding);

        } catch (UnsupportedEncodingException e) {
            throw new APPErrorException("字符串转换失败", e);
        } catch (IOException e) {
            throw new APPErrorException("输入信息获取错误", e);
        } finally {
            if (null != outStream) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将输入的流转换成 字节
     * @param p_Stream
     * @return
     * @throws APPErrorException
     */
    private byte[] parseByte(InputStream p_Stream) throws APPErrorException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        byte[] data = new byte[4096];
        try {
            int count = -1;

            while ((count = p_Stream.read(data, 0, 4096)) != -1)
                outStream.write(data, 0, count);

            data = null;
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new APPErrorException("获取输入的信息出错", e);
        }
        finally {
            if(null!=outStream)
            {
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将参数 转换成 a=1&b=2的形式
     * @param p_Paramss
     * @return
     */
    private String getParamsString(Map<String, String> p_Paramss) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> item : p_Paramss.entrySet()) {
            String key = item.getKey();
            String value = item.getValue();

            sb.append(key + "=" + value + "&");

        }
        return StringUtil.removeLast(sb.toString(), '&');
    }

    /**
     * 设置请求头
     * @param p_Conn
     */
    private void setRequestHead(URLConnection p_Conn) {
        for (Map.Entry<String, String> item : m_Head.entrySet()) {
            p_Conn.setRequestProperty(item.getKey(), item.getValue());
        }
        if(m_Cookies.size()>0) {
            StringBuilder sb =new StringBuilder();
            for(int i=0;i<m_Cookies.size();i++){
                if(i==0)
                    sb.append(m_Cookies.get(i));
                sb.append("; "+m_Cookies.get(i));

            }
            p_Conn.setRequestProperty("Cookie",sb.toString() );
        }
    }

    /**
     * 获取响应头中 字符编码 如果不存在 则使用 设置的encoding
     * @param m_ReponseHead
     * @return
     */
    private  String getResponseEncoding(Map<String,List<String>> m_ReponseHead)
    {
        List<String> names= m_ReponseHead.get("Content-Type");
        for(int i=0;i<names.size();i++) {
            String name =names.get(i);
            if (name.indexOf("charset=") >= 0) {
                String[] codes = name.split("charset=");
                if(StringUtil.isNullOrEmpty(codes[1])) {
                    return codes[1];
                }
            }
        }
        return m_Encoding;
    }

    /**
     * 判断响应数据是否为GZIP
     * @param m_ReponseHead
     * @return
     */
    private  boolean isGzip(Map<String,List<String>> m_ReponseHead)
    {
        Object value= m_ReponseHead.get("content-encoding");
        String valuestr=null!=value?value.toString():"";

        return valuestr.indexOf("gzip")>-1;
    }

    /**
     * 请求
     * @param p_Params
     * @return
     * @throws APPErrorException
     */
    private URLConnection doInvoke(String p_Params) throws APPErrorException {

        String url = m_Url;
        if ("POST".equals(m_Method.toUpperCase())) {

        } else if ("GET".equals(m_Method.toUpperCase())&&!StringUtil.isNullOrEmpty(p_Params)) {
            url += "?" + p_Params;
        }


        PrintWriter out = null;
        try {


            URL realUrl = new URL(url);

            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            setRequestHead(conn);


            if ("POST".equals(m_Method.toUpperCase())) {
                // 发送POST请求必须设置如下两行
                conn.setDoOutput(true);
                conn.setDoInput(true);
                // 获取URLConnection对象对应的输te出流
                out = new PrintWriter(conn.getOutputStream());
                // 发送请求参数
                out.print(p_Params);
                // flush输出流的缓冲
                out.flush();
                // 定义BufferedReader输入流来读取URL的响应

            } else if ("GET".equals(m_Method.toUpperCase())) {
                conn.connect();
            }
            List<String> cookies= conn.getHeaderFields().get("Set-Cookie");
            if(null!=cookies) {
                m_Cookies.addAll(cookies);
            }
            return conn;

        } catch (Exception e) {
            throw new APPErrorException("请求出错！", e);
        }
    }
    public static void main(String[] args)
    {
        WebClient client =new WebClient();
        client.setUrl("http://172.26.6.196:8081/NJDWYXQK/Dwyxqk_NJ.jsp");
        try {
           String ss= client.doInvokeAsString("");
            System.out.println(ss);
        } catch (APPErrorException e) {
            e.printStackTrace();
        }

    }
}
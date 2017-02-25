package tgtools.service;

import java.util.Date;

import tgtools.tasks.Task;
import tgtools.util.DateUtil;

public abstract class BaseService extends Task {

    public BaseService()
    {
    	m_IsStop = true;
    }
    /// <summary>
    /// Milliseconds 
    /// </summary>
    protected abstract int getInterval();
    protected abstract Date getEndTime();

    public Date m_LastTime;
    public boolean m_IsStop; 
    
    public Date getLastTime() {
    	return m_LastTime;
    }
    public void setLastTime(Date p_LastTime) {
    	this.m_LastTime = p_LastTime;
    }
    public boolean isStop() {
    	return m_IsStop;
    }
    public void setIsStop(boolean p_IsStop) {
    	this.m_IsStop = p_IsStop;
    }
    
    public void start()
    {
        if (m_IsStop)
        {
        	runThread(null);
            
        	m_IsStop = false;
        }
    }

    public void stop()
    {
        m_IsStop = true;
    }

    /// <summary>
    /// 是否可以运行
    /// </summary>
    /// <returns></returns>
    public boolean canRun()
    {
        if (!m_IsStop)
        {
            return false;
        }
        if (isBusy())
        {
            return false;
        }
        
        //第一次运行不用判断时间周期
        if (m_LastTime == null)
        {
            return true;
        }

        if (DateUtil.getCurrentDate().getTime()==getEndTime().getTime())
        {
            return false;
        }
        if (DateUtil.getCurrentDate().getTime() >= DateUtil.addSeconds(m_LastTime, getInterval()/1000).getTime())
        {
            return true;
        }

        return false;
    }
   @Override
    public  boolean canCancel()
    {
         return false; 
    }
    
   @Override
    protected void onError(Exception p_Exception)
    {
        this.m_IsStop = true;
        super.onError(p_Exception);
    }

}


package Server.Common;

public class Snapshot {

    private String m_itemKey;
    private RMItem m_item;

    public Snapshot(String itemKey, RMItem item){
        m_itemKey = itemKey;
        m_item = item;
    }

    public String getItemKey() {
        return m_itemKey;
    }

    public RMItem getItem() {
        return m_item;
    }

    public boolean equals(Object o)
    {
        if (o == null)
            return false;

        if (o instanceof Snapshot)
        {
            return ((Snapshot)o).getItemKey().equals(m_itemKey);
        }

        return false;
    }
}
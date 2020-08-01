package cn.connor.service.impl;

import cn.connor.annotation.Autowired;
import cn.connor.annotation.Service;
import cn.connor.annotation.Transactional;
import cn.connor.dao.AccountDao;
import cn.connor.pojo.Account;
import cn.connor.service.TransferService;

/**
 * @author 应癫
 */
@SuppressWarnings("AlibabaTransactionMustHaveRollback")
@Service("TransferService")
public class TransferServiceImpl implements TransferService {
    // 最佳状态
    @Autowired
    private AccountDao accountDao;

    @Override
    @Transactional
    public void transfer(String fromCardNo, String toCardNo, int money) throws Exception {
        Account from = accountDao.queryAccountByCardNo(fromCardNo);
        Account to = accountDao.queryAccountByCardNo(toCardNo);

        from.setMoney(from.getMoney() - money);
        to.setMoney(to.getMoney() + money);

        accountDao.updateAccountByCardNo(to);
        int c = 1/0;
        accountDao.updateAccountByCardNo(from);
    }
}

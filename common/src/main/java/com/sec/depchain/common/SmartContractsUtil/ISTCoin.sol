// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";


contract ISTCoin is ERC20 {
    //IBlacklist public blacklist;

    function decimals() public view virtual override returns (uint8) {
        return 2;
    }

    constructor() ERC20("IST Coin", "IST") {
        _mint(msg.sender, 100_000_000 * 10 ** decimals());
        //blacklist = IBlacklist(blacklistAddress);
    }

    //function setBlacklistContract(address blacklistAddress) external onlyOwner {
    //    blacklist = IBlacklist(blacklistAddress);
    //}

    function transfer(address recipient, uint256 amount) public override returns (bool) {
        //require(!blacklist.isBlacklisted(msg.sender), "Sender is blacklisted");
        //require(!blacklist.isBlacklisted(recipient), "Recipient is blacklisted");
        return super.transfer(recipient, amount);
    }

    function transferFrom(address sender, address recipient, uint256 amount) public override returns (bool) {
        //require(!blacklist.isBlacklisted(sender), "Sender is blacklisted");
        //require(!blacklist.isBlacklisted(recipient), "Recipient is blacklisted");
        return super.transferFrom(sender, recipient, amount);
    }
}
